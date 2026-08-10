package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AgentMessageProxy {

    private static final long TOOL_WAIT_TIMEOUT_MS = 60_000;
    private static final long TOOL_POLL_INTERVAL_MS = 200;
    private static final char[] CONVERSATION_ID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz_".toCharArray();
    private static final int CONVERSATION_ID_LENGTH = 24;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ChatService chatService;
    private final ToolExecutionService toolExecutionService;
    private ChatDataCacheManager chatDataCacheManager;

    public AgentMessageProxy(ChatService chatService, ToolExecutionService toolExecutionService) {
        this.chatService = chatService;
        this.toolExecutionService = toolExecutionService;
    }

    public void setChatDataCacheManager(ChatDataCacheManager chatDataCacheManager) {
        this.chatDataCacheManager = chatDataCacheManager;
    }

    public Message sendUserMessage(String childSessionId, String content, String modelId, Boolean thinking) {
        ChatRequest request = ChatRequest.builder()
                .sessionId(childSessionId)
                .content(content)
                .modelId(modelId)
                .thinking(thinking)
                .build();
        return processChat(request);
    }

    /**
     * 向会话发送用户消息，自动生成 24 位 conversationId 标识对话归属。
     *
     * @param sessionId 会话 ID
     * @param content   用户消息内容
     * @param modelId   模型 ID（可为 null）
     * @param thinking  是否启用思考模式（可为 null 表示默认行为）
     * @return 最终 assistant 回复消息
     */
    public Message sendUserMessageToSession(String sessionId, String content, String modelId, Boolean thinking) {
        ChatRequest request = ChatRequest.builder()
                .sessionId(sessionId)
                .content(content)
                .modelId(modelId)
                .thinking(thinking)
                .conversationId(generateConversationId())
                .build();
        return processChat(request);
    }

    private static String generateConversationId() {
        StringBuilder sb = new StringBuilder(CONVERSATION_ID_LENGTH);
        for (int i = 0; i < CONVERSATION_ID_LENGTH; i++) {
            sb.append(CONVERSATION_ID_CHARS[RANDOM.nextInt(CONVERSATION_ID_CHARS.length)]);
        }
        return sb.toString();
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

    private Message processToolCalls(String sessionId, Map<String, Integer> toolCallCounts) {
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

    private void waitForToolCompletion(String sessionId, String toolId) {
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
