package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AgentMessageProxy {

    private static final long TOOL_WAIT_TIMEOUT_MS = 60_000;
    private static final long TOOL_POLL_INTERVAL_MS = 200;
    private final ChatService chatService;
    private final ToolExecutionService toolExecutionService;

    public AgentMessageProxy(ChatService chatService, ToolExecutionService toolExecutionService) {
        this.chatService = chatService;
        this.toolExecutionService = toolExecutionService;
    }

    public Message sendUserMessage(Long childSessionId, String content, Long modelId) {
        ChatRequest request = ChatRequest.builder()
                .sessionId(childSessionId)
                .content(content)
                .modelId(modelId)
                .build();
        return processChat(request);
    }

    private Message processChat(ChatRequest request) {
        checkReactorThread();
        Flux<ServerSentEvent<ChatChunk>> flux = chatService.chat(request);
        List<ServerSentEvent<ChatChunk>> events = flux.collectList().block();

        CollectedResult result = collectContent(events);

        if (result.hasToolCalls()) {
            Map<String, Integer> toolCallCounts = new HashMap<>();
            return processToolCalls(request.getSessionId(), toolCallCounts);
        }

        return Message.builder()
                .role("assistant")
                .content(result.content())
                .build();
    }

    private Message processToolCalls(Long sessionId, Map<String, Integer> toolCallCounts) {
        while (true) {
            ToolExecutionService.ToolExecutionResult execResult = toolExecutionService.executeTool(sessionId);
            String status = execResult.status();
            if ("empty".equals(status)) {
                break;
            }
            if ("executing".equals(status)) {
                waitForToolCompletion(sessionId, execResult.toolId());
            } else {
                log.warn("sessionId={} 工具执行返回非预期状态: {} toolId={}", sessionId, status, execResult.toolId());
            }

            String toolKey = execResult.toolName() + ":" + execResult.arguments();
            int count = toolCallCounts.merge(toolKey, 1, Integer::sum);
            if (count >= 5) {
                log.warn("sessionId={} 工具 {} 同一参数组合调用次数达到 {}，超过阈值 5，终止", sessionId, toolKey, count);
                return Message.builder()
                        .role("assistant")
                        .content("")
                        .build();
            }

            if (!execResult.hasMore()) {
                break;
            }
        }

        checkReactorThread();
        Flux<ServerSentEvent<ChatChunk>> contFlux = toolExecutionService.continueAfterTools(sessionId);
        List<ServerSentEvent<ChatChunk>> contEvents = contFlux.collectList().block();

        CollectedResult contResult = collectContent(contEvents);

        if (contResult.hasToolCalls()) {
            return processToolCalls(sessionId, toolCallCounts);
        }

        return Message.builder()
                .role("assistant")
                .content(contResult.content())
                .build();
    }

    private void waitForToolCompletion(Long sessionId, String toolId) {
        long deadline = System.currentTimeMillis() + TOOL_WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            ToolExecutionService.ToolStatusResult status = toolExecutionService.getToolStatus(sessionId, toolId);
            String s = status.status();
            if ("idle".equals(s) || "done".equals(s) || "failed".equals(s)) {
                return;
            }
            try {
                Thread.sleep(TOOL_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("sessionId={} toolId={} 等待超时", sessionId, toolId);
    }

    private static void checkReactorThread() {
        if (Schedulers.isInNonBlockingThread()) {
            throw new IllegalStateException("AgentMessageProxy.block() 不能在 Reactor 非阻塞线程中调用");
        }
    }

    private static CollectedResult collectContent(List<ServerSentEvent<ChatChunk>> events) {
        if (events == null || events.isEmpty()) {
            return new CollectedResult("", false);
        }
        StringBuilder content = new StringBuilder();
        boolean hasToolCalls = false;
        for (ServerSentEvent<ChatChunk> event : events) {
            ChatChunk chunk = event.data();
            if (chunk == null) continue;
            if (chunk.getDelta() != null) {
                content.append(chunk.getDelta());
            }
            if (chunk.getHasToolCalls() != null && chunk.getHasToolCalls()) {
                hasToolCalls = true;
            }
        }
        return new CollectedResult(content.toString(), hasToolCalls);
    }

    private record CollectedResult(String content, boolean hasToolCalls) {
    }
}
