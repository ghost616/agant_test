package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.util.JsonMapper;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.invoker.HookData;
import com.ghost616.agentbase.service.agent.invoker.HookManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.ToolCallQueueManager;
import com.ghost616.agentbase.service.agent.invoker.ToolHookContext;
import com.ghost616.agentbase.service.agent.invoker.ToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ToolExecutionService {

    private final AgentComponentRegistry registry;
    private final ChatService chatService;
    private ToolCallQueueManager toolCallQueueManager;
    private ToolManager toolManager;
    private SystemToolManager systemToolManager;
    private SessionManager sessionManager;
    private AgentContextManager agentContextManager;
    private ToolExecutionTracker toolExecutionTracker;
    private HookManager hookManager;
    private volatile boolean initialized;

    public ToolExecutionService(AgentComponentRegistry registry, ChatService chatService) {
        this.registry = registry;
        this.chatService = chatService;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    toolCallQueueManager = registry.getToolCallQueueManager();
                    toolManager = registry.getToolManager();
                    systemToolManager = registry.getSystemToolManager();
                    sessionManager = registry.getSessionManager();
                    agentContextManager = registry.getAgentContextManager();
                    toolExecutionTracker = registry.getToolExecutionTracker();
                    hookManager = registry.getHookManager();
                    initialized = true;
                }
            }
        }
    }

    public record ToolExecutionResult(String status, String toolId, String toolName,
                                      String arguments, boolean hasMore, String message) {
    }

    public record ToolStatusResult(String status, String toolId, String toolName,
                                    String arguments, boolean hasMore, String result) {
    }

    public ToolExecutionResult executeTool(Long sessionId) {
        ensureInitialized();
        MessageDataProvider.ToolCallData peekData = toolCallQueueManager.peek(sessionId);
        if (peekData == null) {
            return new ToolExecutionResult("empty", null, null, null, false, null);
        }

        String peekToolCallName = peekData.toolCallName();
        ToolInvoker invoker = null;
        try {
            if (peekToolCallName.startsWith("_sys_")) {
                String sysToolName = peekToolCallName.substring("_sys_".length());
                invoker = systemToolManager.getSystemTool(sysToolName);
            } else {
                invoker = toolManager.getInvoker(sessionId, peekToolCallName);
            }
        } catch (Exception e) {
            log.error("sessionId={} 获取工具调用器失败, toolName={}", sessionId, peekToolCallName, e);
            return new ToolExecutionResult("failed", peekData.toolCallId(), peekToolCallName,
                    peekData.toolCallArguments(), toolCallQueueManager.hasPending(sessionId), e.getMessage());
        }

        if (invoker == null) {
            toolCallQueueManager.poll(sessionId);
            boolean hasMore = toolCallQueueManager.hasPending(sessionId);
            return new ToolExecutionResult("failed", peekData.toolCallId(), peekToolCallName,
                    peekData.toolCallArguments(), hasMore, "工具调用器不存在");
        }

        MessageDataProvider.ToolCallData toolCall = toolCallQueueManager.poll(sessionId);
        boolean hasMore = toolCallQueueManager.hasPending(sessionId);

        if (toolCall == null) {
            return new ToolExecutionResult("empty", null, null, null, false, null);
        }

        AgentContextManager.AgentSessionContext sessionCtx = agentContextManager.get(sessionId);
        if (sessionCtx == null) {
            return new ToolExecutionResult("error", null, null, null, false, "session not found");
        }
        AgentExecutionContext context = sessionCtx.context();
        if (context.isStopped()) {
            toolCallQueueManager.clear(sessionId);
            toolExecutionTracker.clear(sessionId);
            return new ToolExecutionResult("empty", null, null, null, false, null);
        }

        final String toolCallId = toolCall.toolCallId();
        final String toolCallName = toolCall.toolCallName();
        final String toolCallArguments = toolCall.toolCallArguments();
        final ToolInvoker capturedInvoker = invoker;
        final AgentExecutionContext capturedContext = context;

        toolExecutionTracker.setExecuting(sessionId, toolCallId, toolCallName, toolCallArguments, hasMore);

        HookData beforeHookData = new HookData(new ToolHookContext(toolCallId, toolCallName, toolCallArguments, null));
        hookManager.triggerSessionHooks(sessionId, HookPhase.BEFORE_TOOL_CALL, capturedContext, beforeHookData);
        hookManager.triggerHooks(HookPhase.BEFORE_TOOL_CALL, capturedContext, beforeHookData);
        hookManager.executePostHooks(capturedContext, beforeHookData);

        CompletableFuture.supplyAsync(() -> {
            try {
                String res = toolManager.execute(capturedInvoker, capturedContext, toolCallArguments);
                toolExecutionTracker.setDone(sessionId, toolCallId, res);

                HookData afterHookData = new HookData(new ToolHookContext(toolCallId, toolCallName, toolCallArguments, res));
                hookManager.triggerSessionHooks(sessionId, HookPhase.AFTER_TOOL_CALL, capturedContext, afterHookData);
                hookManager.triggerHooks(HookPhase.AFTER_TOOL_CALL, capturedContext, afterHookData);
                hookManager.executePostHooks(capturedContext, afterHookData);

                return res;
            } catch (Exception e) {
                log.error("sessionId={} 工具执行异常, toolName={}", sessionId, toolCallName, e);
                toolExecutionTracker.setFailed(sessionId, toolCallId, e.getMessage());
                return null;
            }
        });

        return new ToolExecutionResult("executing", toolCallId, toolCallName, toolCallArguments, hasMore, null);
    }

    public ToolStatusResult getToolStatus(Long sessionId, String toolId) {
        ensureInitialized();
        ToolExecutionTracker.ToolExecutionStatus status = toolExecutionTracker.getCurrentExecution(sessionId, toolId);
        if (status == null) {
            return new ToolStatusResult("idle", null, null, null, false, null);
        }
        return new ToolStatusResult(status.status(), status.currentToolId(), status.currentToolName(),
                status.currentArguments(), status.hasMore(), status.result());
    }

    public Flux<ServerSentEvent<ChatChunk>> continueAfterTools(Long sessionId) {
        ensureInitialized();

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
                String toolResultJson = JsonMapper.MAPPER.writeValueAsString(toolResultMap);
                sessionManager.messageSave().sessionId(sessionId).role("tool")
                        .content(r.result()).toolCallId(r.toolId()).toolResult(toolResultJson).save();
            } catch (Exception e) {
                log.error("sessionId={} 构建 toolResult JSON 失败", sessionId, e);
            }
        }

        if (!results.isEmpty()) {
            for (ToolExecutionTracker.ToolResult r : results) {
                AgentExecutionContext.HistoryEntry entry = new AgentExecutionContext.HistoryEntry(
                        "tool", r.result(), null, r.toolId(),
                        0, LocalDateTime.now(), Collections.emptyList(),
                        null);
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
