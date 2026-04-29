package com.testtarget.controller;

import com.testtarget.model.DeserializeLog;
import com.testtarget.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping("/logs")
    public Map<String, Object> getLogs(@RequestParam(defaultValue = "50") int count) {
        List<DeserializeLog> logs = logService.getRecentLogs(count);
        long successCount = logs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long errorCount = logs.stream().filter(l -> "ERROR".equals(l.getStatus())).count();

        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        result.put("total", logs.size());
        result.put("successCount", successCount);
        result.put("errorCount", errorCount);
        return result;
    }

    @GetMapping(value = "/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return logService.subscribe();
    }
}
