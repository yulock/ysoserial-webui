package com.yulock.payload.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yulock.payload.entity.TestTarget;
import com.yulock.payload.mapper.TestTargetMapper;
import com.yulock.payload.service.TestTargetService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestTargetServiceImpl extends ServiceImpl<TestTargetMapper, TestTarget> implements TestTargetService {

    @Override
    public List<TestTarget> getByUserId(Long userId) {
        return lambdaQuery()
                .eq(TestTarget::getUserId, userId)
                .orderByDesc(TestTarget::getCreateTime)
                .list();
    }
}

