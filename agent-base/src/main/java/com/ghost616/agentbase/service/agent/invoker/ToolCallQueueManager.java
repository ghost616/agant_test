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

    public void enqueue(Long sessionId, List<MessageDataProvider.ToolCallData> toolCalls) {
        provider.enqueue(sessionId, toolCalls);
    }

    public MessageDataProvider.ToolCallData poll(Long sessionId) {
        return provider.poll(sessionId);
    }

    public MessageDataProvider.ToolCallData peek(Long sessionId) {
        return provider.peek(sessionId);
    }

    public boolean hasPending(Long sessionId) {
        return provider.hasPending(sessionId);
    }

    public void clear(Long sessionId) {
        provider.clearQueue(sessionId);
    }
}
