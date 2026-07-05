package com.ghost616.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghost616.platform.entity.Session;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
