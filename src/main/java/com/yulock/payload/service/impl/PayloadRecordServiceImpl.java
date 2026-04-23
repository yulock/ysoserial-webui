package com.yulock.payload.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yulock.payload.entity.PayloadRecord;
import com.yulock.payload.mapper.PayloadRecordMapper;
import com.yulock.payload.service.PayloadRecordService;
import org.springframework.stereotype.Service;

@Service
public class PayloadRecordServiceImpl extends ServiceImpl<PayloadRecordMapper, PayloadRecord> implements PayloadRecordService {
}
