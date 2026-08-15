package com.ghost616.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghost616.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User 的 MyBatis-Plus Mapper 接口。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
