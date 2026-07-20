package com.ghost616.agentbase.service.agent;

import java.util.List;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToolExecutionTracker {

    private final ToolExecutionProvider provider;

    public ToolExecutionTracker(AgentComponentRegistry registry) {
        this.provider = registry.getToolExecutionProvider();
    }

    public record ToolExecutionStatus(String currentToolId, String currentToolName,
                                       String currentArguments, String status,
                                       String result, boolean hasMore) {
    }

    public record ToolResult(String toolId, String toolName, String arguments, String result) {
    }

    public void setExecuting(Long sessionId, String toolId, String toolName,
                               String arguments, boolean hasMore) {
        provider.updateExecution(sessionId, new ToolExecutionStatus(
                toolId, toolName, arguments, "executing", null, hasMore));
    }

    public void setDone(Long sessionId, String toolId, String result) {
        ToolExecutionStatus current = provider.getCurrentExecution(sessionId, toolId);
        if (current == null) {
            return;
        }
        provider.updateExecution(sessionId, new ToolExecutionStatus(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "done", result, current.hasMore()));
        log.debug("sessionId={} 工具执行完成, toolName={}, result={}", sessionId, current.currentToolName(), result);
    }

    public void setFailed(Long sessionId, String toolId, String error) {
        ToolExecutionStatus current = provider.getCurrentExecution(sessionId, toolId);
        if (current == null) {
            return;
        }
        provider.updateExecution(sessionId, new ToolExecutionStatus(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "failed", error, current.hasMore()));
    }

    public void clear(Long sessionId) {
        provider.clearTracking(sessionId);
    }

    public ToolExecutionStatus getCurrentExecution(Long sessionId, String toolId) {
        return provider.getCurrentExecution(sessionId, toolId);
    }

    public List<ToolResult> getAndClearResults(Long sessionId) {
        return provider.getAndClearResults(sessionId);
    }
}
