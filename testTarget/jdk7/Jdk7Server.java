package com.testtarget;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

public class Jdk7Server {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void main(String[] args) throws Exception {
        System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", "true");

        int port = 8081;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        server.createContext("/CC1", new DeserializeHandler("CC1"));
        server.createContext("/CC3", new DeserializeHandler("CC3"));
        server.createContext("/JDK7", new DeserializeHandler("JDK7"));
        server.createContext("/health", new HealthHandler());

        server.start();
        System.out.println("[JDK7 Server] Started on port " + port);
        System.out.println("[JDK7 Server] Endpoints: /CC1 /CC3 /JDK7");
    }

    static class DeserializeHandler implements HttpHandler {
        private final String chain;

        DeserializeHandler(String chain) {
            this.chain = chain;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String sourceIp = exchange.getRemoteAddress().getAddress().getHostAddress();
            long startTime = System.currentTimeMillis();
            String status = "PENDING";
            String message = "";
            int payloadSize = 0;

            try {
                InputStream is = exchange.getRequestBody();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                byte[] payload = baos.toByteArray();
                payloadSize = payload.length;

                log("RECV", chain, sourceIp, payloadSize, "Receiving payload...");

                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payload));
                ois.readObject();
                ois.close();

                long elapsed = System.currentTimeMillis() - startTime;
                status = "SUCCESS";
                message = "Deserialization SUCCESS - RCE triggered. Time: " + elapsed + "ms";
                log(status, chain, sourceIp, payloadSize, message);

                String response = "OK - " + chain + " deserialization completed (" + elapsed + "ms)";
                sendResponse(exchange, 200, response);

            } catch (Throwable e) {
                long elapsed = System.currentTimeMillis() - startTime;
                status = "ERROR";
                String cause = e.getCause() != null ? e.getCause().toString() : e.toString();
                message = "Deserialization FAILED: " + cause;
                log(status, chain, sourceIp, payloadSize, message);

                String response = "ERROR - " + chain + ": " + cause;
                sendResponse(exchange, 200, response);
            }
        }

        private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] resp = body.getBytes("UTF-8");
            exchange.sendResponseHeaders(code, resp.length);
            OutputStream os = exchange.getResponseBody();
            os.write(resp);
            os.close();
        }

        private void log(String status, String chain, String ip, int size, String msg) {
            String ts = sdf.format(new Date());
            System.out.println(String.format("[%s] [%s] [%s] %s | %dB | %s",
                    ts, status, chain, ip, size, msg));
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String resp = "Test Target Running - JDK 7";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, resp.length());
            OutputStream os = exchange.getResponseBody();
            os.write(resp.getBytes());
            os.close();
        }
    }
}
