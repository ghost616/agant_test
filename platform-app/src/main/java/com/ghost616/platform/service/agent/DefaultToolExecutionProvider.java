package com.ghost616.platform.service.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionTracker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultToolExecutionProvider implements ToolExecutionProvider {

    private final ConcurrentHashMap<Long, Deque<MessageDataProvider.ToolCallData>> toolCallQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ToolExecutionTracker.ToolExecutionStatus> currentExecutions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ToolExecutionTracker.ToolResult>> completedResults = new ConcurrentHashMap<>();

    private static String key(Long sessionId, String toolId) {
        return sessionId + "_" + toolId;
    }

    @Override
    public void enqueue(Long sessionId, List<MessageDataProvider.ToolCallData> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }
        Deque<MessageDataProvider.ToolCallData> queue = toolCallQueues.computeIfAbsent(sessionId,
                k -> new ArrayDeque<>());
        queue.addAll(toolCalls);
        log.debug("sessionId={} 已入队 {} 个工具调用", sessionId, toolCalls.size());
    }

    @Override
    public MessageDataProvider.ToolCallData poll(Long sessionId) {
        Deque<MessageDataProvider.ToolCallData> queue = toolCallQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.pollFirst();
    }

    @Override
    public MessageDataProvider.ToolCallData peek(Long sessionId) {
        Deque<MessageDataProvider.ToolCallData> queue = toolCallQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.peekFirst();
    }

    @Override
    public boolean hasPending(Long sessionId) {
        Deque<MessageDataProvider.ToolCallData> queue = toolCallQueues.get(sessionId);
        return queue != null && !queue.isEmpty();
    }

    @Override
    public void clearQueue(Long sessionId) {
        toolCallQueues.remove(sessionId);
        log.debug("sessionId={} 已清理工具调用队列", sessionId);
    }

    @Override
    public void updateExecution(Long sessionId, ToolExecutionTracker.ToolExecutionStatus status) {
        String k = key(sessionId, status.currentToolId());
        switch (status.status()) {
            case "executing":
                currentExecutions.put(k, status);
                completedResults.computeIfAbsent(k, kk -> Collections.synchronizedList(new ArrayList<>()));
                break;
            case "done":
            case "failed": {
                ToolExecutionTracker.ToolExecutionStatus current = currentExecutions.get(k);
                if (current == null) {
                    log.warn("sessionId={} toolId={} 未找到当前执行状态，无法更新为 {}", sessionId, status.currentToolId(), status.status());
                    return;
                }
                currentExecutions.put(k, status);
                String resultMsg = "failed".equals(status.status()) ? "[error] " + status.result() : status.result();
                completedResults.get(k).add(new ToolExecutionTracker.ToolResult(
                        current.currentToolId(), current.currentToolName(),
                        current.currentArguments(), resultMsg));
                log.debug("sessionId={} 工具执行{}, toolName={}, result={}",
                        sessionId, "done".equals(status.status()) ? "完成" : "失败",
                        current.currentToolName(), status.result());
                break;
            }
            default:
                log.warn("sessionId={} toolId={} 未知状态: {}", sessionId, status.currentToolId(), status.status());
        }
    }

    @Override
    public void clearTracking(Long sessionId) {
        String prefix = sessionId + "_";
        currentExecutions.keySet().removeIf(k -> k.startsWith(prefix));
        completedResults.keySet().removeIf(k -> k.startsWith(prefix));
    }

    @Override
    public ToolExecutionTracker.ToolExecutionStatus getCurrentExecution(Long sessionId, String toolId) {
        return currentExecutions.get(key(sessionId, toolId));
    }

    @Override
    public List<ToolExecutionTracker.ToolResult> getAndClearResults(Long sessionId) {
        String prefix = sessionId + "_";
        List<ToolExecutionTracker.ToolResult> results = new ArrayList<>();
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
