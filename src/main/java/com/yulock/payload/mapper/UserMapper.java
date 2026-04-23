package com.yulock.payload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yulock.payload.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
