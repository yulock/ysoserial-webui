package com.yulock.payload.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.yulock.payload.entity.TestTarget;
import com.yulock.payload.service.TestTargetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/target")
public class TargetController {

    @Autowired
    private TestTargetService testTargetService;

    @GetMapping("/list")
    public SaResult list() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<TestTarget> targets = testTargetService.getByUserId(userId);
        return SaResult.ok().setData(targets);
    }

    @PostMapping("/save")
    public SaResult save(@RequestBody TestTarget target) {
        Long userId = StpUtil.getLoginIdAsLong();
        target.setUserId(userId);
        if (target.getId() != null && target.getId() > 0) {
            testTargetService.updateById(target);
        } else {
            testTargetService.save(target);
        }
        return SaResult.ok("保存成功").setData(target);
    }

    @DeleteMapping("/delete/{id}")
    public SaResult delete(@PathVariable Long id) {
        testTargetService.removeById(id);
        return SaResult.ok("删除成功");
    }
}

