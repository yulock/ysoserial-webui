package com.yulock.payload.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yulock.payload.entity.User;
import com.yulock.payload.mapper.UserMapper;
import com.yulock.payload.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Override
    public User login(String username, String password) {
        User user = getByUsername(username);
        if (user == null) {
            return null;
        }
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!encryptedPassword.equals(user.getPassword())) {
            return null;
        }
        return user;
    }
    
    @Override
    public User register(String username, String password, String email) {
        if (getByUsername(username) != null) {
            return null;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setEmail(email);
        save(user);
        return user;
    }
    
    @Override
    public User getByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }
}
