package com.ghost616.agentbase.sendmessage;

import lombok.Getter;

@Getter
public class VariableMessage extends SessionMessage {

    private final String scope;
    private final String key;
    private final String value;
    private final String operation;

    public VariableMessage(Long sessionId, String scope, String key, String value, String operation) {
        setSessionId(sessionId);
        this.scope = scope;
        this.key = key;
        this.value = value;
        this.operation = operation;
    }

    @Override
    public String getMessageName() {
        return "SESSION".equals(scope) ? MessageName.SESSION_VARIABLE : MessageName.CONVERSATION_VARIABLE;
    }
}
