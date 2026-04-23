package com.yulock.payload.service;

import java.util.List;
import java.util.Map;

public interface PayloadService {
    List<Map<String, Object>> getAvailableGadgets();
    byte[] generatePayload(String gadgetName, String command);
    String testPayload(byte[] payload, String host, Integer port, String type);
}
