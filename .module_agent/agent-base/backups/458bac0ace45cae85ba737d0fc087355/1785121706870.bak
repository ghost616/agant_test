package com.ghost616.agentbase.sendmessage;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import lombok.Getter;

@Getter
public class HistoryMessage extends SessionMessage {

    private final AgentExecutionContext.HistoryEntry historyEntry;

    public HistoryMessage(Long sessionId, AgentExecutionContext.HistoryEntry historyEntry) {
        setSessionId(sessionId);
        this.historyEntry = historyEntry;
    }

    @Override
    public String getMessageName() {
        return MessageName.HISTORY_MESSAGE;
    }
}
