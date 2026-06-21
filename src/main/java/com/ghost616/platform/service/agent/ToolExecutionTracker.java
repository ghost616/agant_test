package com.ghost616.platform.service.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ToolExecutionTracker {

    private final ConcurrentHashMap<Long, ToolExecutionStatus> currentExecutions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<ToolResult>> completedResults = new ConcurrentHashMap<>();

    public record ToolExecutionStatus(String currentToolId, String currentToolName,
                                       String currentArguments, String status,
                                       String result, boolean hasMore) {
    }

    public record ToolResult(String toolId, String toolName, String arguments, String result) {
    }

    public void setExecuting(Long sessionId, String toolId, String toolName,
                              String arguments, boolean hasMore) {
        currentExecutions.put(sessionId, new ToolExecutionStatus(
                toolId, toolName, arguments, "executing", null, hasMore));
        completedResults.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()));
    }

    public void setDone(Long sessionId, String result) {
        ToolExecutionStatus current = currentExecutions.get(sessionId);
        if (current == null) {
            return;
        }
        ToolExecutionStatus done = new ToolExecutionStatus(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "done", result, current.hasMore());
        currentExecutions.put(sessionId, done);
        completedResults.get(sessionId).add(new ToolResult(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), result));
        log.info("sessionId={} 工具执行完成, toolName={}, result={}", sessionId, current.currentToolName(), result);
    }

    public void setFailed(Long sessionId, String error) {
        ToolExecutionStatus current = currentExecutions.get(sessionId);
        if (current == null) {
            return;
        }
        ToolExecutionStatus failed = new ToolExecutionStatus(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "failed", error, current.hasMore());
        currentExecutions.put(sessionId, failed);
        completedResults.get(sessionId).add(new ToolResult(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "[error] " + error));
    }

    public void clear(Long sessionId) {
        currentExecutions.remove(sessionId);
        completedResults.remove(sessionId);
    }

    public ToolExecutionStatus getCurrentExecution(Long sessionId) {
        return currentExecutions.get(sessionId);
    }

    public List<ToolResult> getAndClearResults(Long sessionId) {
        List<ToolResult> results = completedResults.remove(sessionId);
        currentExecutions.remove(sessionId);
        return results != null ? results : Collections.emptyList();
    }
}
