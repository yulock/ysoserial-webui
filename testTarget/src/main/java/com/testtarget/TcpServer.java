package com.testtarget;

import com.testtarget.model.DeserializeLog;
import com.testtarget.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class TcpServer {

    private static final int TCP_PORT = 9090;

    @Autowired
    private LogService logService;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
                System.out.println("[TCP Server] Listening on port " + TCP_PORT);
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handle(socket)).start();
                }
            } catch (Exception e) {
                System.err.println("[TCP Server] Failed: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handle(Socket socket) {
        String sourceIp = socket.getInetAddress().getHostAddress();
        DeserializeLog log = new DeserializeLog();
        log.setChain("RAW-TCP");
        log.setSourceIp(sourceIp);
        log.setStatus("PENDING");
        log.setMessage("TCP connection accepted. Starting deserialization...");
        logService.addLog(log);

        long start = System.currentTimeMillis();
        try {
            socket.setSoTimeout(10000);
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ois.readObject();

            long elapsed = System.currentTimeMillis() - start;
            log.setElapsedMs(elapsed);
            log.setStatus("SUCCESS");
            log.setMessage("TCP deserialization SUCCESS - RCE triggered. Time: " + elapsed + "ms");
            logService.addLog(log);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.setElapsedMs(elapsed);
            log.setStatus("ERROR");
            String cause = e.getCause() != null ? e.getCause().toString() : e.toString();
            log.setMessage("TCP deserialization FAILED: " + cause);
            logService.addLog(log);
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
}
