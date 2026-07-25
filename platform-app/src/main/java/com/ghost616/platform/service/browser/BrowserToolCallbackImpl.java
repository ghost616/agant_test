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

    public BrowserToolTask getTask(String sessionId, String toolConfigId) {
        return taskMap.get(sessionId + ":" + toolConfigId);
    }

    @Override
    public String execute(String sessionId, String toolConfigId, String toolName, String toolParams) {
        String key = sessionId + ":" + toolConfigId;
        BrowserToolTask task = BrowserToolTask.builder()
                .sessionId(sessionId)
                .toolId(toolConfigId)
                .toolName(toolName)
                .toolParams(toolParams)
                .toolResult(new CompletableFuture<>())
                .build();
        taskMap.put(key, task);
        try {
            return task.getToolResult().get(600, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("BrowserToolCallback execute interrupted for sessionId={} toolConfigId={}", sessionId, toolConfigId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.warn("BrowserToolCallback execute failed for sessionId={} toolConfigId={}", sessionId, toolConfigId, e);
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            log.warn("BrowserToolCallback execute timed out for sessionId={} toolConfigId={}", sessionId, toolConfigId, e);
            throw new RuntimeException(e);
        } finally {
            taskMap.remove(key);
        }
    }
}
