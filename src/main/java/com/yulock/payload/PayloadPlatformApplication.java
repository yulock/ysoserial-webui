package com.yulock.payload;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PayloadPlatformApplication {
    public static void main(String[] args) {
        // 启用Apache Commons Collections的不安全序列化（用于CC链Payload生成）
        System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", "true");
        SpringApplication.run(PayloadPlatformApplication.class, args);
    }
}
