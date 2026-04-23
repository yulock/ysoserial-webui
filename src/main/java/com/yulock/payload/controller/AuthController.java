package com.yulock.payload.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import com.yulock.payload.entity.User;
import com.yulock.payload.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/login")
    public SaResult login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        
        User user = userService.login(username, password);
        if (user == null) {
            return SaResult.error("用户名或密码错误");
        }
        
        StpUtil.login(user.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("avatar", user.getAvatar());
        
        return SaResult.ok("登录成功").setData(result);
    }
    
    @PostMapping("/register")
    public SaResult register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String email = params.get("email");
        
        if (userService.getByUsername(username) != null) {
            return SaResult.error("用户名已存在");
        }
        
        User user = userService.register(username, password, email);
        if (user == null) {
            return SaResult.error("注册失败");
        }
        
        return SaResult.ok("注册成功");
    }
    
    @PostMapping("/logout")
    public SaResult logout() {
        StpUtil.logout();
        return SaResult.ok("退出成功");
    }
    
    @GetMapping("/info")
    public SaResult info() {
        if (!StpUtil.isLogin()) {
            return SaResult.error("未登录");
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("avatar", user.getAvatar());
        result.put("createTime", user.getCreateTime());
        
        return SaResult.ok().setData(result);
    }
}
