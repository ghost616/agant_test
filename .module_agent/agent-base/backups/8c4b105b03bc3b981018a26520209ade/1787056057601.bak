package com.ghost616.agentbase.sendmessage;

import lombok.Getter;

/**
 * 发送用户消息事件类，继承 SessionMessage。
 * 由 AgentContextManager.sendUserMessage() 在持久化子会话用户消息后触发发送，
 * 携带 conversationId、parentSessionId 与 mainSessionId（沿父会话链向上追溯的主会话 ID），
 * 供外部系统感知用户消息已发送至子会话。
 */
@Getter
public class SendUserMessage extends SessionMessage {

    private final String content;

    public SendUserMessage(String sessionId, String content, String conversationId,
                           String parentSessionId, String mainSessionId) {
        setSessionId(sessionId);
        this.content = content;
        setConversationId(conversationId);
        setParentSessionId(parentSessionId);
        setMainSessionId(mainSessionId);
    }

    @Override
    public String getMessageName() {
        return MessageName.SEND_USER_MESSAGE;
    }
}
