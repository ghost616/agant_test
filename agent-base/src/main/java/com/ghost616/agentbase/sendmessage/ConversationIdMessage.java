package com.ghost616.agentbase.sendmessage;

import lombok.Getter;

@Getter
public class ConversationIdMessage extends SessionMessage {

    private final String conversationId;

    public ConversationIdMessage(String sessionId, String conversationId) {
        setSessionId(sessionId);
        this.conversationId = conversationId;
    }

    @Override
    public String getMessageName() {
        return MessageName.CONVERSATION_ID;
    }
}