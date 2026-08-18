package com.ghost616.agentbase.sendmessage;

import com.ghost616.agentbase.service.agent.AgentExecutionContext.ChildSession;
import lombok.Getter;

@Getter
public class ChildCreateSession extends SessionMessage {

    private final ChildSession childSession;

    public ChildCreateSession(String parentSessionId, ChildSession childSession) {
        this.childSession = childSession;
        setSessionId(parentSessionId);
        setParentSessionId(parentSessionId);
    }

    @Override
    public String getMessageName() {
        return MessageName.CHILD_SESSION;
    }
}
