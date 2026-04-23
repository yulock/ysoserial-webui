package com.yulock.payload.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yulock.payload.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);
    User register(String username, String password, String email);
    User getByUsername(String username);
}
