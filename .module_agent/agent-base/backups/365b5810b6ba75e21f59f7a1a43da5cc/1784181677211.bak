package com.ghost616.agentbase.service.agent.invoker;


import com.ghost616.agentbase.dto.model.Message;


/**
 * 子会话回调接口，定义子会话消息执行的契约。
 *
 * @author ghost616
 */
@FunctionalInterface
public interface SubSessionCallback {

    /**
     * 执行子会话消息处理。
     *
     * @param sessionId   会话 ID
     * @param userMessage 用户消息内容
     * @return 执行结果消息
     */
    Message execute(Long sessionId, String userMessage);
}
