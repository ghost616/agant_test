package com.ghost616.platform.service.browser;

import com.ghost616.agentinteg.tool.BrowserToolCallback;
import com.ghost616.platform.dto.browser.BrowserToolTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class BrowserToolCallbackImpl implements BrowserToolCallback {

    private final ConcurrentHashMap<String, BrowserToolTask> taskMap = new ConcurrentHashMap<>();

    public BrowserToolTask getTask(String sessionId, String toolId) {
        return taskMap.get(sessionId + ":" + toolId);
    }

    @Override
    public String execute(String sessionId, String toolId, String toolName, String toolParams) {
        String key = sessionId + ":" + toolId;
        BrowserToolTask task = BrowserToolTask.builder()
                .sessionId(sessionId)
                .toolId(toolId)
                .toolName(toolName)
                .toolParams(toolParams)
                .toolResult(new CompletableFuture<>())
                .build();
        taskMap.put(key, task);
        try {
            return task.getToolResult().get(600, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("BrowserToolCallback execute interrupted for sessionId={} toolId={}", sessionId, toolId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.warn("BrowserToolCallback execute failed for sessionId={} toolId={}", sessionId, toolId, e);
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            log.warn("BrowserToolCallback execute timed out for sessionId={} toolId={}", sessionId, toolId, e);
            throw new RuntimeException(e);
        } finally {
            taskMap.remove(key);
        }
    }
}
