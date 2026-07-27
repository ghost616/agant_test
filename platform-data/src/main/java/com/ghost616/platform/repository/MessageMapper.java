package com.ghost616.platform.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import com.ghost616.platform.entity.Message;


@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Update("UPDATE message SET rollback=1 WHERE session_id = #{sessionId} AND sequence_num >= #{sequenceNum}")
    int rollbackBySessionIdAndGeSequenceNum(Long sessionId, Integer sequenceNum);
}
