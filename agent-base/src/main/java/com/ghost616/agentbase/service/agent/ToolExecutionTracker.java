package com.ghost616.agentbase.service.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToolExecutionTracker {

    private final ConcurrentHashMap<String, ToolExecutionStatus> currentExecutions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ToolResult>> completedResults = new ConcurrentHashMap<>();

    private static String key(Long sessionId, String toolId) {
        return sessionId + "_" + toolId;
    }

    public record ToolExecutionStatus(String currentToolId, String currentToolName,
                                       String currentArguments, String status,
                                       String result, boolean hasMore) {
    }

    public record ToolResult(String toolId, String toolName, String arguments, String result) {
    }

    public void setExecuting(Long sessionId, String toolId, String toolName,
                              String arguments, boolean hasMore) {
        String k = key(sessionId, toolId);
        currentExecutions.put(k, new ToolExecutionStatus(
                toolId, toolName, arguments, "executing", null, hasMore));
        completedResults.computeIfAbsent(k, kk -> Collections.synchronizedList(new ArrayList<>()));
    }

    public void setDone(Long sessionId, String toolId, String result) {
        String k = key(sessionId, toolId);
        ToolExecutionStatus current = currentExecutions.get(k);
        if (current == null) {
            return;
        }
        ToolExecutionStatus done = new ToolExecutionStatus(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "done", result, current.hasMore());
        currentExecutions.put(k, done);
        completedResults.get(k).add(new ToolResult(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), result));
        log.debug("sessionId={} 工具执行完成, toolName={}, result={}", sessionId, current.currentToolName(), result);
    }

    public void setFailed(Long sessionId, String toolId, String error) {
        String k = key(sessionId, toolId);
        ToolExecutionStatus current = currentExecutions.get(k);
        if (current == null) {
            return;
        }
        ToolExecutionStatus failed = new ToolExecutionStatus(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "failed", error, current.hasMore());
        currentExecutions.put(k, failed);
        completedResults.get(k).add(new ToolResult(
                current.currentToolId(), current.currentToolName(),
                current.currentArguments(), "[error] " + error));
    }

    public void clear(Long sessionId) {
        String prefix = sessionId + "_";
        currentExecutions.keySet().removeIf(k -> k.startsWith(prefix));
        completedResults.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public ToolExecutionStatus getCurrentExecution(Long sessionId, String toolId) {
        return currentExecutions.get(key(sessionId, toolId));
    }

    public List<ToolResult> getAndClearResults(Long sessionId) {
        String prefix = sessionId + "_";
        List<ToolResult> results = new ArrayList<>();
        completedResults.keySet().removeIf(k -> {
            if (k.startsWith(prefix)) {
                results.addAll(completedResults.get(k));
                return true;
            }
            return false;
        });
        currentExecutions.keySet().removeIf(k -> k.startsWith(prefix));
        return results.isEmpty() ? Collections.emptyList() : results;
    }
}
