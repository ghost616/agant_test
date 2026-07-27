package com.ghost616.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghost616.platform.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {

    @Update("UPDATE session SET total_token_used = COALESCE(total_token_used, 0) + #{tokens} WHERE id = #{sessionId}")
    int addTotalTokenUsed(@Param("sessionId") Long sessionId, @Param("tokens") Long tokens);
}
