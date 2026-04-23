package com.yulock.payload.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public SaResult handleNotLoginException(NotLoginException e) {
        log.warn("Token invalid: {}", e.getMessage());
        return SaResult.error("登录已过期，请重新登录").setCode(401);
    }

    @ExceptionHandler(Exception.class)
    public SaResult handleException(Exception e) {
        log.error("System error: ", e);
        return SaResult.error("系统错误：" + e.getMessage());
    }
}
