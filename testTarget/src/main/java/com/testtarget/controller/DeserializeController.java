package com.testtarget.controller;

import com.testtarget.model.DeserializeLog;
import com.testtarget.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;

@RestController
public class DeserializeController {

    @Autowired
    private LogService logService;

    /* ========== JDK 8 环境下可用的利用链 (在 jdk8 service 中暴露) ========== */

    private String handle(String chain, HttpServletRequest request) {
        DeserializeLog log = new DeserializeLog();
        log.setChain(chain);
        log.setSourceIp(request.getRemoteAddr());
        log.setStatus("PENDING");
        log.setMessage("Starting deserialization...");
        logService.addLog(log);

        long start = System.currentTimeMillis();
        try {
            InputStream is = request.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            byte[] payload = baos.toByteArray();
            log.setPayloadSize(payload.length);

            new ObjectInputStream(new java.io.ByteArrayInputStream(payload)).readObject();

            long elapsed = System.currentTimeMillis() - start;
            log.setElapsedMs(elapsed);
            log.setStatus("SUCCESS");
            log.setMessage(String.format("Deserialization SUCCESS — RCE triggered. Processing time: %dms", elapsed));
            logService.addLog(log);

            return "OK - " + chain + " deserialization completed (" + elapsed + "ms)";
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.setElapsedMs(elapsed);
            log.setStatus("ERROR");
            String cause = e.getCause() != null ? e.getCause().toString() : e.toString();
            log.setMessage(String.format("Deserialization FAILED: %s", cause));
            logService.addLog(log);

            return "ERROR - " + chain + ": " + cause;
        }
    }

    @PostMapping("/CC2")
    public String cc2(HttpServletRequest request) { return handle("CC2", request); }

    @PostMapping("/CC4")
    public String cc4(HttpServletRequest request) { return handle("CC4", request); }

    @PostMapping("/CC5")
    public String cc5(HttpServletRequest request) { return handle("CC5", request); }

    @PostMapping("/CC6")
    public String cc6(HttpServletRequest request) { return handle("CC6", request); }

    @PostMapping("/CC7")
    public String cc7(HttpServletRequest request) { return handle("CC7", request); }

    @PostMapping("/CB1")
    public String cb1(HttpServletRequest request) { return handle("CB1", request); }

    @PostMapping("/SP1")
    public String sp1(HttpServletRequest request) { return handle("SP1", request); }

    @PostMapping("/SP2")
    public String sp2(HttpServletRequest request) { return handle("SP2", request); }

    @PostMapping("/C3P0")
    public String c3p0(HttpServletRequest request) { return handle("C3P0", request); }

    @PostMapping("/AJW")
    public String ajw(HttpServletRequest request) { return handle("AJW", request); }

    @PostMapping("/GRV")
    public String grv(HttpServletRequest request) { return handle("GRV", request); }

    @PostMapping("/URL")
    public String urldns(HttpServletRequest request) { return handle("URL", request); }

    @PostMapping("/ROME")
    public String rome(HttpServletRequest request) { return handle("ROME", request); }

    @GetMapping("/health")
    public String health() {
        return "Test Target Running — JDK 8";
    }
}
