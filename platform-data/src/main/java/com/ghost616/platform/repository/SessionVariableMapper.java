package com.ghost616.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghost616.platform.entity.SessionVariable;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionVariableMapper extends BaseMapper<SessionVariable> {
}
