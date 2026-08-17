package com.ghost616.agentbase.sendmessage;

/**
 * ConversationIdMessage 消息类，继承 SessionMessage，messageName=CONVERSATION_ID。
 * conversationId 由父类 SessionMessage 承载。
 */
public class ConversationIdMessage extends SessionMessage {

    public ConversationIdMessage(String sessionId, String conversationId) {
        setSessionId(sessionId);
        setConversationId(conversationId);
    }

    @Override
    public String getMessageName() {
        return MessageName.CONVERSATION_ID;
    }
}