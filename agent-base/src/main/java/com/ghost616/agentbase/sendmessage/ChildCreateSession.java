package com.ghost616.agentbase.sendmessage;

import com.ghost616.agentbase.service.agent.AgentExecutionContext.ChildSession;
import lombok.Getter;

@Getter
public class ChildCreateSession extends SessionMessage {

    private final Long parentSessionId;
    private final ChildSession childSession;

    public ChildCreateSession(Long parentSessionId, ChildSession childSession) {
        this.parentSessionId = parentSessionId;
        this.childSession = childSession;
        this.setSessionId(parentSessionId);
    }

    @Override
    public String getMessageName() {
        return MessageName.CHILD_SESSION;
    }
}
