package com.ghost616.agentbase.service.agent;

import java.util.List;

public interface ToolExecutionProvider {

    void enqueue(Long sessionId, List<MessageDataProvider.ToolCallData> toolCalls);

    MessageDataProvider.ToolCallData poll(Long sessionId);

    MessageDataProvider.ToolCallData peek(Long sessionId);

    boolean hasPending(Long sessionId);

    void clearQueue(Long sessionId);

    void updateExecution(Long sessionId, ToolExecutionTracker.ToolExecutionStatus status);

    void clearTracking(Long sessionId);

    ToolExecutionTracker.ToolExecutionStatus getCurrentExecution(Long sessionId, String toolId);

    List<ToolExecutionTracker.ToolResult> getAndClearResults(Long sessionId);
}
