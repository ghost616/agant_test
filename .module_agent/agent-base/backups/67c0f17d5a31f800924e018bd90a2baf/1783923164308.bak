package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
public class AgentMessageProxy {

    private static final long TOOL_WAIT_TIMEOUT_MS = 60_000;
    private static final long TOOL_POLL_INTERVAL_MS = 200;
    private static final int MAX_TOOL_ROUNDS = 10;

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
            return processToolCalls(request.getSessionId(), 1);
        }

        return Message.builder()
                .role("assistant")
                .content(result.content())
                .build();
    }

    private Message processToolCalls(Long sessionId, int round) {
        if (round > MAX_TOOL_ROUNDS) {
            log.warn("sessionId={} 工具调用达到最大轮次上限{}，终止", sessionId, MAX_TOOL_ROUNDS);
            return Message.builder()
                    .role("assistant")
                    .content("")
                    .build();
        }

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
            if (!execResult.hasMore()) {
                break;
            }
        }

        checkReactorThread();
        Flux<ServerSentEvent<ChatChunk>> contFlux = toolExecutionService.continueAfterTools(sessionId);
        List<ServerSentEvent<ChatChunk>> contEvents = contFlux.collectList().block();

        CollectedResult contResult = collectContent(contEvents);

        if (contResult.hasToolCalls()) {
            return processToolCalls(sessionId, round + 1);
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
