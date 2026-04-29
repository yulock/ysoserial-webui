package com.testtarget.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class DeserializeLog {
    private Long id;
    private String chain;
    private String sourceIp;
    private int payloadSize;
    private long elapsedMs;
    private String status;      // SUCCESS, ERROR, PENDING
    private String message;
    private String timestamp;

    private static long counter = 0;

    public DeserializeLog() {
        this.id = ++counter;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public static synchronized long nextId() {
        return ++counter;
    }
}
