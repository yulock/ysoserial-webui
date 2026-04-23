package com.yulock.payload.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payload_record")
public class PayloadRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String gadgetChain;
    
    private String command;
    
    private String payloadType;
    
    @TableField(value = "payload_data", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private byte[] payloadData;
    
    private String result;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
