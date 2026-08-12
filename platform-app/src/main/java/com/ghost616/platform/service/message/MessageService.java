package com.ghost616.platform.service.message;

import com.ghost616.platform.entity.Message;

import java.util.List;

/**
 * 消息查询服务接口，提供消息持久化数据的查询能力。
 */
public interface MessageService {

    /**
     * 查询指定会话的全部有效消息（过滤已回滚消息），按 sequenceNum 升序返回。
     *
     * @param sessionId 会话 ID
     * @return 消息实体列表，不做 memoryPoint 记忆点过滤
     */
    List<Message> getAllMessages(Long sessionId);
}
