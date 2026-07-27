package com.ghost616.agentbase.sendmessage;

import com.ghost616.agentbase.dto.model.Message;
import lombok.Getter;

@Getter
public class ChildMessageEvent extends SessionMessage {

    private final Long childSessionId;
    private final String content;
    private final Long modelId;
    private final Boolean thinking;
    private final Message result;

    public ChildMessageEvent(Long sessionId, Long childSessionId, String content, Long modelId, Boolean thinking, Message result) {
        setSessionId(sessionId);
        this.childSessionId = childSessionId;
        this.content = content;
        this.modelId = modelId;
        this.thinking = thinking;
        this.result = result;
    }

    @Override
    public String getMessageName() {
        return MessageName.CHILD_MESSAGE;
    }
}
