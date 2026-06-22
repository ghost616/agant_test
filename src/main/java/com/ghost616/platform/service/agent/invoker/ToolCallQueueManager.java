package com.ghost616.platform.service.agent.invoker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ghost616.platform.service.agent.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ToolCallQueueManager {

    private final ConcurrentHashMap<Long, Deque<SessionManager.ToolCallData>> toolCallQueues = new ConcurrentHashMap<>();

    public void enqueue(Long sessionId, List<SessionManager.ToolCallData> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }
        Deque<SessionManager.ToolCallData> queue = toolCallQueues.computeIfAbsent(sessionId,
                k -> new ArrayDeque<>());
        queue.addAll(toolCalls);
        log.info("sessionId={} 已入队 {} 个工具调用", sessionId, toolCalls.size());
    }

    public SessionManager.ToolCallData poll(Long sessionId) {
        Deque<SessionManager.ToolCallData> queue = toolCallQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.pollFirst();
    }

    public SessionManager.ToolCallData peek(Long sessionId) {
        Deque<SessionManager.ToolCallData> queue = toolCallQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.peekFirst();
    }

    public boolean hasPending(Long sessionId) {
        Deque<SessionManager.ToolCallData> queue = toolCallQueues.get(sessionId);
        return queue != null && !queue.isEmpty();
    }

    public void clear(Long sessionId) {
        toolCallQueues.remove(sessionId);
        log.debug("sessionId={} 已清理工具调用队列", sessionId);
    }
}
