package com.ghost616.agentbase.service.agent.invoker;

import java.util.List;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToolCallQueueManager {

    private final ToolExecutionProvider provider;

    public ToolCallQueueManager(AgentComponentRegistry registry) {
        this.provider = registry.getToolExecutionProvider();
    }

    public void enqueue(String sessionId, List<MessageDataProvider.ToolCallData> toolCalls) {
        provider.enqueue(sessionId, toolCalls);
    }

    public MessageDataProvider.ToolCallData poll(String sessionId) {
        return provider.poll(sessionId);
    }

    public MessageDataProvider.ToolCallData peek(String sessionId) {
        return provider.peek(sessionId);
    }

    public boolean hasPending(String sessionId) {
        return provider.hasPending(sessionId);
    }

    public void clear(String sessionId) {
        provider.clearQueue(sessionId);
    }
}
