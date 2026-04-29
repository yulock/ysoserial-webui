package com.yulock.payload.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yulock.payload.entity.TestTarget;

import java.util.List;

public interface TestTargetService extends IService<TestTarget> {
    List<TestTarget> getByUserId(Long userId);
}

