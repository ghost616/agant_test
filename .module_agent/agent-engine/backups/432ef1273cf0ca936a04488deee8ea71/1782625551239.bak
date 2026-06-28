package com.ghost616.platform.service.agent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import com.ghost616.platform.dto.chat.ChatRequest;
import com.ghost616.platform.dto.model.ChatChunk;
import com.ghost616.platform.dto.model.Message;
import com.ghost616.platform.dto.model.ToolCall;
import com.ghost616.platform.dto.model.ToolDefinition;
import com.ghost616.platform.dto.tool.ToolConfigDTO;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.enums.HookPhase;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.agent.invoker.SystemPostHook;
import com.ghost616.platform.service.hook.HookInvoker;
import com.ghost616.platform.service.hook.SystemHook;
import com.ghost616.platform.service.model.invoker.ModelInvoker;
import com.ghost616.platform.service.model.invoker.ModelInvokerManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    public static final String TOOL_CONTINUE_MARKER = "[tool_continue]";

    private final AgentContextManager agentContextManager;
    private final SessionManager sessionManager;
    private final ModelInvokerManager modelInvokerManager;
    private final ModelConfigMapper modelConfigMapper;
    private final SessionMapper sessionMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    private final Map<HookPhase, List<HookInvoker>> systemHooks = new HashMap<>();
    private final List<HookInvoker> systemPostHooks = new ArrayList<>();

    @PostConstruct
    void initSystemHooks() {
        Map<String, HookInvoker> beans = applicationContext.getBeansOfType(HookInvoker.class);
        for (HookInvoker hook : beans.values()) {
            HookPhase phase = hook.getPhase();
            if (hook instanceof SystemPostHook) {
                systemPostHooks.add(hook);
            } else {
                systemHooks.computeIfAbsent(phase, k -> new ArrayList<>()).add(hook);
            }
        }
    }

    private void triggerHooks(HookPhase phase, AgentExecutionContext ctx, ChatChunk chunk) {
        List<HookInvoker> hooks = systemHooks.get(phase);
        if (hooks != null) {
            hooks.stream()
                    .sorted(Comparator.comparingInt(h -> ((SystemHook) h).getIndex()))
                    .forEach(h -> h.execute(ctx, chunk));
        }
    }

    private void executePostHooks(AgentExecutionContext ctx, ChatChunk chunk) {
        systemPostHooks.stream()
                .sorted(Comparator.comparingInt(h -> ((SystemHook) h).getIndex()))
                .forEach(h -> h.execute(ctx, chunk));
    }

    public Flux<ServerSentEvent<ChatChunk>> chat(ChatRequest request) {
        Long sessionId = request.getSessionId();
        String content = request.getContent();
        Long modelId = request.getModelId();

        AgentContextManager.AgentSessionContext sessionContext =
                agentContextManager.getOrCreate(sessionId, modelId);
        AgentExecutionContext context = sessionContext.context();
        AgentExecutionContext.AgentContextMutator contextMutator = sessionContext.mutator();

        boolean isToolContinue = TOOL_CONTINUE_MARKER.equals(content);

        if (!isToolContinue) {
            contextMutator.clearConversationVariables();
            sessionManager.saveMessage(sessionId, "user", content, null, null, null);

            AgentExecutionContext.HistoryEntry userEntry = new AgentExecutionContext.HistoryEntry(
                    "user", content, null, null,
                    context.getHistory().size() + 1,
                    LocalDateTime.now(),
                    Collections.emptyList());
            contextMutator.addHistoryEntry(userEntry);
        }

        if (modelId != null && !modelId.equals(context.getModelId())) {
            Session session = sessionMapper.selectById(sessionId);
            if (session != null) {
                session.setModelId(modelId);
                sessionMapper.updateById(session);
            }
            contextMutator.setModelId(modelId);
        }

        Long finalModelId = (modelId != null) ? modelId : context.getModelId();
        ModelConfig modelConfig = modelConfigMapper.selectById(finalModelId);
        if (modelConfig == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }

        triggerHooks(HookPhase.SESSION_START, context, null);

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role("system")
                .content(context.getSystemPrompt() != null ? context.getSystemPrompt() : "")
                .build());

        List<AgentExecutionContext.HistoryEntry> historyEntries = context.getHistory();
        Integer recentCount = context.getRecentMessageCount();
        if (recentCount != null && recentCount > 0 && recentCount < historyEntries.size()) {
            historyEntries = truncateByPairs(historyEntries, recentCount);
        }

        for (AgentExecutionContext.HistoryEntry entry : historyEntries) {
            Message.MessageBuilder builder = Message.builder()
                    .role(entry.role())
                    .content(entry.content());
            if (entry.toolCalls() != null && !entry.toolCalls().isEmpty()) {
                builder.toolCalls(entry.toolCalls());
            }
            if (entry.reasoning() != null && !entry.reasoning().isEmpty()
                    && entry.toolCalls() != null && !entry.toolCalls().isEmpty()) {
                builder.reasoning(entry.reasoning());
            }
            if (entry.toolCallId() != null) {
                builder.toolCallId(entry.toolCallId());
            }
            messages.add(builder.build());
        }

        ModelInvoker invoker = modelInvokerManager.getInvoker(modelConfig);

        List<ToolDefinition> tools = context.getTools().stream()
                .map(invoker::toToolDefinition)
                .toList();

        com.ghost616.platform.dto.model.ChatRequest chatRequest =
                com.ghost616.platform.dto.model.ChatRequest.builder()
                        .messages(messages)
                        .tools(tools)
                        .thinking(request.getThinking())
                        .build();

        AtomicBoolean hasToolCalls = new AtomicBoolean(false);

        Flux<ChatChunk> stream = invoker.invokeStream(chatRequest);

        return stream
                .doOnNext(chunk -> {
                    if (chunk.getToolCalls() != null && !chunk.getToolCalls().isEmpty()) {
                        hasToolCalls.set(true);
                    }
                    if (chunk.getFinishReason() != null) {
                        chunk.setHasToolCalls(hasToolCalls.get());
                    }
                    triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, context, chunk);
                    executePostHooks(context, chunk);
                })
                .map(chunk -> ServerSentEvent.<ChatChunk>builder()
                        .data(chunk)
                        .build())
                .doOnComplete(() -> {
                    ChatChunk completeChunk = ChatChunk.builder()
                            .hasToolCalls(hasToolCalls.get())
                            .build();
                    triggerHooks(HookPhase.AFTER_MESSAGE_RECEIVE, context, completeChunk);
                    executePostHooks(context, completeChunk);
                });
    }

    private static List<AgentExecutionContext.HistoryEntry> truncateByPairs(
            List<AgentExecutionContext.HistoryEntry> history, int pairCount) {
        List<Integer> userIndices = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            if ("user".equals(history.get(i).role())) {
                userIndices.add(i);
            }
        }
        if (userIndices.size() <= pairCount) {
            return history;
        }
        int startIndex = userIndices.get(userIndices.size() - pairCount);
        return history.subList(startIndex, history.size());
    }
}
