package com.ghost616.platform.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.service.agent.ChatService;
import com.ghost616.platform.service.agent.ToolExecutionTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.platform.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.ToolCallQueueManager;
import com.ghost616.agentbase.service.agent.invoker.ToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;


@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ToolExecutionController {

    private final ToolCallQueueManager toolCallQueueManager;
    private final ToolManager toolManager;
    private final SystemToolManager systemToolManager;
    private final SessionManager sessionManager;
    private final ChatService chatService;
    private final AgentContextManager agentContextManager;
    private final ToolExecutionTracker toolExecutionTracker;
    private final ObjectMapper objectMapper;

    @PostMapping("/{sessionId}/execute-tools")
    public ApiResponse<Map<String, Object>> executeTools(@PathVariable Long sessionId) {
        MessageDataProvider.ToolCallData peekData = toolCallQueueManager.peek(sessionId);

        if (peekData == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "empty");
            result.put("hasMore", false);
            return ApiResponse.success(result);
        }

        String peekToolCallName = peekData.toolCallName();
        ToolInvoker invoker;
        try {
            if (peekToolCallName.startsWith("_sys_")) {
                String sysToolName = peekToolCallName.substring("_sys_".length());
                invoker = systemToolManager.getSystemTool(sysToolName);
                if (invoker == null) {
                    throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR, "系统工具不存在: " + sysToolName);
                }
            } else {
                invoker = toolManager.getInvoker(sessionId, peekToolCallName);
            }
        } catch (Exception e) {
            log.error("sessionId={} 获取工具调用器失败, toolName={}", sessionId, peekToolCallName, e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "failed");
            result.put("toolId", peekData.toolCallId());
            result.put("toolName", peekToolCallName);
            result.put("arguments", peekData.toolCallArguments());
            result.put("hasMore", toolCallQueueManager.hasPending(sessionId));
            result.put("message", e.getMessage());
            return ApiResponse.success(result);
        }

        MessageDataProvider.ToolCallData toolCall = toolCallQueueManager.poll(sessionId);
        boolean hasMore = toolCallQueueManager.hasPending(sessionId);

        if (toolCall == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "empty");
            result.put("hasMore", false);
            return ApiResponse.success(result);
        }

        AgentContextManager.AgentSessionContext sessionCtx = agentContextManager.get(sessionId);
        if (sessionCtx == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "session not found");
            return ApiResponse.success(result);
        }
        AgentExecutionContext context = sessionCtx.context();
        if (context.isStopped()) {
            toolCallQueueManager.clear(sessionId);
            toolExecutionTracker.clear(sessionId);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "empty");
            result.put("hasMore", false);
            return ApiResponse.success(result);
        }
        final String toolCallId = toolCall.toolCallId();
        final String toolCallName = toolCall.toolCallName();
        final String toolCallArguments = toolCall.toolCallArguments();

        toolExecutionTracker.setExecuting(sessionId, toolCallId,
                toolCallName, toolCallArguments, hasMore);

        CompletableFuture.supplyAsync(() -> {
            try {
                String res = toolManager.execute(invoker, context, toolCallArguments);
                toolExecutionTracker.setDone(sessionId, res);
                return res;
            } catch (Exception e) {
                log.error("sessionId={} 工具执行异常, toolName={}", sessionId, toolCallName, e);
                toolExecutionTracker.setFailed(sessionId, e.getMessage());
                return null;
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("status", "executing");
        result.put("toolId", toolCallId);
        result.put("toolName", toolCallName);
        result.put("arguments", toolCallArguments);
        result.put("hasMore", hasMore);
        return ApiResponse.success(result);
    }

    @GetMapping("/{sessionId}/tool-status")
    public ApiResponse<Map<String, Object>> toolStatus(@PathVariable Long sessionId) {
        ToolExecutionTracker.ToolExecutionStatus status = toolExecutionTracker.getCurrentExecution(sessionId);
        if (status == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "idle");
            return ApiResponse.success(result);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", status.status());
        result.put("toolId", status.currentToolId());
        result.put("toolName", status.currentToolName());
        result.put("arguments", status.currentArguments());
        result.put("hasMore", status.hasMore());
        if (status.result() != null) {
            result.put("result", status.result());
        }
        return ApiResponse.success(result);
    }

    @PostMapping(value = "/{sessionId}/continue", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatChunk>> continueChat(@PathVariable Long sessionId) {
        ToolExecutionTracker.ToolExecutionStatus status = toolExecutionTracker.getCurrentExecution(sessionId);
        if (status != null && "executing".equals(status.status())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "仍有工具正在执行中");
        }

        AgentContextManager.AgentSessionContext sessionCtx = agentContextManager.get(sessionId);
        if (sessionCtx != null && sessionCtx.context().isStopped()) {
            toolCallQueueManager.clear(sessionId);
            toolExecutionTracker.clear(sessionId);
            return Flux.empty();
        }

        List<ToolExecutionTracker.ToolResult> results = toolExecutionTracker.getAndClearResults(sessionId);

        for (ToolExecutionTracker.ToolResult r : results) {
            try {
                Map<String, String> toolResultMap = new HashMap<>();
                toolResultMap.put("toolName", r.toolName());
                toolResultMap.put("arguments", r.arguments());
                toolResultMap.put("result", r.result());
                String toolResultJson = objectMapper.writeValueAsString(toolResultMap);
                sessionManager.save().sessionId(sessionId).role("tool").content(r.result()).toolCallId(r.toolId()).toolResult(toolResultJson).save();
            } catch (Exception e) {
                log.error("sessionId={} 构建 toolResult JSON 失败", sessionId, e);
            }
        }

        if (!results.isEmpty()) {
            for (ToolExecutionTracker.ToolResult r : results) {
                AgentExecutionContext.HistoryEntry entry = new AgentExecutionContext.HistoryEntry(
                        "tool", r.result(), null, r.toolId(),
                        0, LocalDateTime.now(), Collections.emptyList());
                agentContextManager.addHistoryEntry(sessionId, entry);
            }
        }

        toolCallQueueManager.clear(sessionId);
        toolExecutionTracker.clear(sessionId);

        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .content(ChatService.TOOL_CONTINUE_MARKER)
                .build();

        return chatService.chat(request);
    }
}
