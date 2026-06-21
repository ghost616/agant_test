package com.ghost616.platform.service.agent.invoker;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.ghost616.platform.dto.model.ChatChunk;
import com.ghost616.platform.dto.model.ToolCall;
import com.ghost616.platform.dto.model.ToolCallDelta;
import com.ghost616.platform.enums.HookPhase;
import com.ghost616.platform.service.agent.AgentContextManager;
import com.ghost616.platform.service.agent.AgentExecutionContext;
import com.ghost616.platform.service.agent.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSavePostHook implements SystemPostHook {

    private final SessionManager sessionManager;
    private final AgentContextManager agentContextManager;
    private final ToolCallQueueManager toolCallQueueManager;

    private static class ToolAccumulator {
        final StringBuilder id = new StringBuilder();
        final StringBuilder name = new StringBuilder();
        final StringBuilder arguments = new StringBuilder();
    }

    private static class SessionBuffer {
        final StringBuilder contentBuffer = new StringBuilder();
        final StringBuilder reasoningBuffer = new StringBuilder();
        final ConcurrentHashMap<String, ToolAccumulator> toolCallBuffers = new ConcurrentHashMap<>();
    }

    private final ConcurrentHashMap<Long, SessionBuffer> buffers = new ConcurrentHashMap<>();

    @Override
    public HookPhase getPhase() {
        return HookPhase.AFTER_MESSAGE_RECEIVE;
    }

    @Override
    public void execute(AgentExecutionContext ctx, ChatChunk chunk) {
        if (chunk == null) {
            return;
        }
        Long sessionId = ctx.getSessionId();

        if ("stop".equals(chunk.getFinishReason())) {
            SessionBuffer sb = buffers.remove(sessionId);
            if (sb == null) {
                return;
            }
            String content = sb.contentBuffer.toString();
            String reasoning = sb.reasoningBuffer.length() > 0 ? sb.reasoningBuffer.toString() : null;
            List<SessionManager.ToolCallData> toolCalls = null;
            if (!sb.toolCallBuffers.isEmpty()) {
                toolCalls = sb.toolCallBuffers.values().stream()
                        .map(a -> new SessionManager.ToolCallData(
                                a.id.toString(),
                                a.name.toString(),
                                a.arguments.toString()))
                        .collect(Collectors.toList());
            }
            log.info("sessionId={} 保存消息, content={}, reasoning={}, toolCalls数量={}",
                    sessionId, content, reasoning,
                    toolCalls != null ? toolCalls.size() : 0);
            sessionManager.saveMessage(sessionId, "assistant", content, reasoning, null, toolCalls);
            if (toolCalls != null && !toolCalls.isEmpty()) {
                toolCallQueueManager.enqueue(sessionId, toolCalls);
            }
            List<ToolCall> historyToolCalls = null;
            if (!sb.toolCallBuffers.isEmpty()) {
                historyToolCalls = sb.toolCallBuffers.values().stream()
                        .map(a -> ToolCall.builder()
                                .id(a.id.toString())
                                .name(a.name.toString())
                                .arguments(a.arguments.toString())
                                .build())
                        .collect(Collectors.toList());
            }
            agentContextManager.addHistoryEntry(sessionId,
                    new AgentExecutionContext.HistoryEntry(
                            "assistant", content, reasoning, null,
                            ctx.getHistory().size() + 1,
                            LocalDateTime.now(),
                            historyToolCalls != null ? Collections.unmodifiableList(historyToolCalls) : Collections.emptyList()));
            return;
        }

        SessionBuffer sb = buffers.computeIfAbsent(sessionId, k -> new SessionBuffer());
        if (chunk.getDelta() != null) {
            sb.contentBuffer.append(chunk.getDelta());
        }
        if (chunk.getReasoning() != null) {
            sb.reasoningBuffer.append(chunk.getReasoning());
        }
        List<ToolCallDelta> toolCalls = chunk.getToolCalls();
        if (toolCalls != null) {
            for (ToolCallDelta tc : toolCalls) {
                log.info("sessionId={} ToolCallDelta id={} name={} arguments={}",
                        sessionId, tc.getId(), tc.getName(), tc.getArguments());
                String key;
                if (tc.getIndex() != null) {
                    key = String.valueOf(tc.getIndex());
                } else if (tc.getId() != null) {
                    key = tc.getId();
                } else {
                    continue;
                }
                ToolAccumulator acc = sb.toolCallBuffers.get(key);
                if (acc == null || acc.id.length() == 0) {
                    if (acc == null) {
                        acc = new ToolAccumulator();
                        sb.toolCallBuffers.put(key, acc);
                    }
                    acc.id.append(tc.getId());
                    if (tc.getName() != null) {
                        acc.name.append(tc.getName());
                    }
                    if (tc.getArguments() != null) {
                        acc.arguments.append(tc.getArguments());
                    }
                } else {
                    if (tc.getArguments() != null) {
                        acc.arguments.append(tc.getArguments());
                    }
                }
            }
        }
    }
}
