package com.ghost616.agentbase.service.agent;

import java.util.List;

public interface ToolExecutionProvider {

    void enqueue(String sessionId, List<MessageDataProvider.ToolCallData> toolCalls);

    MessageDataProvider.ToolCallData poll(String sessionId);

    MessageDataProvider.ToolCallData peek(String sessionId);

    boolean hasPending(String sessionId);

    void clearQueue(String sessionId);

    void updateExecution(String sessionId, ToolExecutionTracker.ToolExecutionStatus status);

    void clearTracking(String sessionId);

    ToolExecutionTracker.ToolExecutionStatus getCurrentExecution(String sessionId, String toolId);

    List<ToolExecutionTracker.ToolResult> getAndClearResults(String sessionId);
}
