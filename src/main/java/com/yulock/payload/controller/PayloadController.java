package com.yulock.payload.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.yulock.payload.entity.PayloadRecord;
import com.yulock.payload.service.PayloadRecordService;
import com.yulock.payload.service.PayloadService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payload")
public class PayloadController {
    
    @Autowired
    private PayloadService payloadService;
    
    @Autowired
    private PayloadRecordService payloadRecordService;
    
    @GetMapping("/gadgets")
    public SaResult getGadgets() {
        List<Map<String, Object>> gadgets = payloadService.getAvailableGadgets();
        return SaResult.ok().setData(gadgets);
    }
    
    @PostMapping("/generate")
    public SaResult generate(@RequestBody Map<String, String> params) {
        String gadgetName = params.get("gadgetName");
        String command = params.get("command");
        String payloadType = params.getOrDefault("payloadType", "base64");
        
        try {
            byte[] payload = payloadService.generatePayload(gadgetName, command);
            
            // 保存记录
            PayloadRecord record = new PayloadRecord();
            record.setUserId(StpUtil.getLoginIdAsLong());
            record.setGadgetChain(gadgetName);
            record.setCommand(command);
            record.setPayloadType(payloadType);
            record.setPayloadData(payload);
            payloadRecordService.save(record);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", record.getId());
            result.put("size", payload.length);
            
            if ("base64".equals(payloadType)) {
                result.put("payload", Base64.encodeBase64String(payload));
            } else if ("hex".equals(payloadType)) {
                result.put("payload", bytesToHex(payload));
            } else {
                result.put("payload", Base64.encodeBase64String(payload));
            }
            
            return SaResult.ok("生成成功").setData(result);
        } catch (Exception e) {
            return SaResult.error("生成失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/test")
    public SaResult testPayload(@RequestBody Map<String, Object> params) {
        String payloadBase64 = (String) params.get("payload");
        String host = (String) params.get("host");
        Integer port = params.get("port") != null ? ((Number) params.get("port")).intValue() : null;
        String url = (String) params.get("url");
        String type = (String) params.getOrDefault("type", "socket");

        try {
            byte[] payload = Base64.decodeBase64(payloadBase64);
            String result = payloadService.testPayload(payload, host, port, url, type);
            return SaResult.ok().setData(result);
        } catch (Exception e) {
            return SaResult.error("测试失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/history")
    public SaResult getHistory() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<PayloadRecord> records = payloadRecordService.lambdaQuery()
                .eq(PayloadRecord::getUserId, userId)
                .orderByDesc(PayloadRecord::getCreateTime)
                .list();
        return SaResult.ok().setData(records);
    }
    
    @DeleteMapping("/history/{id}")
    public SaResult deleteHistory(@PathVariable Long id) {
        payloadRecordService.removeById(id);
        return SaResult.ok("删除成功");
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
