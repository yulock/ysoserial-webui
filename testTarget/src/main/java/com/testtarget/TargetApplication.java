package com.testtarget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TargetApplication {
    public static void main(String[] args) {
        System.setProperty("org.apache.commons.collections.enableUnsafeSerialization", "true");
        SpringApplication.run(TargetApplication.class, args);
    }
}
