package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.model.ToolInfo;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.invoker.HookData;
import com.ghost616.agentbase.service.agent.invoker.HookManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AgentContextManager agentContextManager;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private SystemToolManager systemToolManager;
    @Mock
    private ToolManager toolManager;
    @Mock
    private ChatDataProvider chatDataProvider;
    @Mock
    private HookManager hookManager;
    @Mock
    private ModelInvoker modelInvoker;
    @Mock
    private AgentContextManager.Builder builder;
    @Mock
    private AgentExecutionContext context;
    @Mock
    private AgentExecutionContext.AgentContextMutator mutator;

    private AgentComponentRegistry registry;
    private ChatService chatService;

    private final String sessionId = "1";

    @BeforeEach
    void setUp() {
        registry = new AgentComponentRegistry();
        registry.setAgentContextManager(agentContextManager);
        registry.setSessionManager(sessionManager);
        registry.setModelInvokerManager(modelInvokerManager);
        registry.setSystemToolManager(systemToolManager);
        registry.setToolManager(toolManager);
        registry.setChatDataProvider(chatDataProvider);
        registry.setHookManager(hookManager);
        chatService = new ChatService(registry);
    }

    @Test
    void constructor_shouldInjectAllDependencies() {
        assertNotNull(chatService);
    }

    @Test
    void chat_SESSION_START阶段triggerSessionHooks优先于triggerHooks调用() {
        ChatRequest request = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        AgentContextManager.AgentSessionContext sessionCtx =
                new AgentContextManager.AgentSessionContext(context, mutator, new java.util.concurrent.atomic.AtomicBoolean(false));

        when(agentContextManager.build(sessionId)).thenReturn(builder);
        when(builder.modelIdOverride(any())).thenReturn(builder);
        when(builder.build()).thenReturn(sessionCtx);
        when(sessionManager.messageSave()).thenReturn(mock(SessionManager.MessageSaveBuilder.class, RETURNS_SELF));
        when(context.getSystemPrompt()).thenReturn("");
        when(context.getHistory()).thenReturn(java.util.Collections.emptyList());
        when(context.getSkills()).thenReturn(null);
        when(context.getTools()).thenReturn(java.util.Collections.emptyList());
        when(context.isMainSession()).thenReturn(false);
        when(systemToolManager.getToolDefinitions()).thenReturn(java.util.Collections.emptyList());
        when(chatDataProvider.getModelConfig(any())).thenReturn(new ModelConfigData("1", "key", "url", "model", 0.7, 4096, "openai", null));
        when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.empty());

        chatService.chat(request);

        verify(hookManager).triggerSessionHooks(sessionId, HookPhase.SESSION_START, context, new HookData((com.ghost616.agentbase.dto.model.ChatChunk) null));
        verify(hookManager).triggerHooks(HookPhase.SESSION_START, context, new HookData((com.ghost616.agentbase.dto.model.ChatChunk) null));
    }

    @Test
    void chat_BEFORE_MESSAGE_SEND阶段triggerSessionHooks在doOnNext中调用() {
        ChatRequest request = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        AgentContextManager.AgentSessionContext sessionCtx =
                new AgentContextManager.AgentSessionContext(context, mutator, new java.util.concurrent.atomic.AtomicBoolean(false));

        when(agentContextManager.build(sessionId)).thenReturn(builder);
        when(builder.modelIdOverride(any())).thenReturn(builder);
        when(builder.build()).thenReturn(sessionCtx);
        when(sessionManager.messageSave()).thenReturn(mock(SessionManager.MessageSaveBuilder.class, RETURNS_SELF));
        when(context.getSystemPrompt()).thenReturn("");
        when(context.getHistory()).thenReturn(java.util.Collections.emptyList());
        when(context.getSkills()).thenReturn(null);
        when(context.getTools()).thenReturn(java.util.Collections.emptyList());
        when(context.isMainSession()).thenReturn(false);
        when(systemToolManager.getToolDefinitions()).thenReturn(java.util.Collections.emptyList());
        when(chatDataProvider.getModelConfig(any())).thenReturn(new ModelConfigData("1", "key", "url", "model", 0.7, 4096, "openai", null));
        when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        com.ghost616.agentbase.dto.model.ChatChunk chunk = com.ghost616.agentbase.dto.model.ChatChunk.builder().delta("hi").build();
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(chunk));

        chatService.chat(request).subscribe();

        verify(hookManager).triggerSessionHooks(sessionId, HookPhase.BEFORE_MESSAGE_SEND, context, new HookData(chunk));
        verify(hookManager).triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, context, new HookData(chunk));
    }

    @Test
    void chat_AFTER_MESSAGE_RECEIVE阶段triggerSessionHooks在doOnComplete中调用() {
        ChatRequest request = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        AgentContextManager.AgentSessionContext sessionCtx =
                new AgentContextManager.AgentSessionContext(context, mutator, new java.util.concurrent.atomic.AtomicBoolean(false));

        when(agentContextManager.build(sessionId)).thenReturn(builder);
        when(builder.modelIdOverride(any())).thenReturn(builder);
        when(builder.build()).thenReturn(sessionCtx);
        when(sessionManager.messageSave()).thenReturn(mock(SessionManager.MessageSaveBuilder.class, RETURNS_SELF));
        when(context.getSystemPrompt()).thenReturn("");
        when(context.getHistory()).thenReturn(java.util.Collections.emptyList());
        when(context.getSkills()).thenReturn(null);
        when(context.getTools()).thenReturn(java.util.Collections.emptyList());
        when(context.isMainSession()).thenReturn(false);
        when(systemToolManager.getToolDefinitions()).thenReturn(java.util.Collections.emptyList());
        when(chatDataProvider.getModelConfig(any())).thenReturn(new ModelConfigData("1", "key", "url", "model", 0.7, 4096, "openai", null));
        when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        com.ghost616.agentbase.dto.model.ChatChunk chunk = com.ghost616.agentbase.dto.model.ChatChunk.builder().delta("hi").finishReason("stop").build();
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(chunk));

        chatService.chat(request).subscribe();

        com.ghost616.agentbase.dto.model.ChatChunk completeChunk = com.ghost616.agentbase.dto.model.ChatChunk.builder().hasToolCalls(false).build();
        verify(hookManager).triggerSessionHooks(sessionId, HookPhase.AFTER_MESSAGE_RECEIVE, context, new HookData(completeChunk));
        verify(hookManager).triggerHooks(HookPhase.AFTER_MESSAGE_RECEIVE, context, new HookData(completeChunk));
    }

    private com.ghost616.agentbase.dto.model.ChatRequest executeFoldChat(
            List<AgentExecutionContext.HistoryEntry> history, Integer recentCount, String expandedIndicesJson) {
        ChatRequest request = ChatRequest.builder().sessionId(sessionId).content("hello").build();
        AgentContextManager.AgentSessionContext sessionCtx =
                new AgentContextManager.AgentSessionContext(context, mutator, new java.util.concurrent.atomic.AtomicBoolean(false));

        when(agentContextManager.build(sessionId)).thenReturn(builder);
        when(builder.modelIdOverride(any())).thenReturn(builder);
        when(builder.build()).thenReturn(sessionCtx);
        when(sessionManager.messageSave()).thenReturn(mock(SessionManager.MessageSaveBuilder.class, RETURNS_SELF));
        when(context.getSystemPrompt()).thenReturn("base_prompt");
        when(context.getHistory()).thenReturn(history);
        when(context.getSkills()).thenReturn(null);
        when(context.getTools()).thenReturn(java.util.Collections.emptyList());
        when(context.isMainSession()).thenReturn(false);
        when(context.getRecentMessageCount()).thenReturn(recentCount);
        lenient().when(context.getConversationVariable(any())).thenReturn(expandedIndicesJson);
        when(systemToolManager.getToolDefinitions()).thenReturn(java.util.Collections.emptyList());
        when(chatDataProvider.getModelConfig(any())).thenReturn(new ModelConfigData("1", "key", "url", "model", 0.7, 4096, "openai", null));
        when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.empty());

        chatService.chat(request);

        ArgumentCaptor<com.ghost616.agentbase.dto.model.ChatRequest> captor =
                ArgumentCaptor.forClass(com.ghost616.agentbase.dto.model.ChatRequest.class);
        verify(modelInvoker).invokeStream(captor.capture());
        return captor.getValue();
    }

    private List<AgentExecutionContext.HistoryEntry> buildFoldHistory(int groupCount) {
        List<AgentExecutionContext.HistoryEntry> history = new ArrayList<>();
        for (int g = 0; g < groupCount - 1; g++) {
            history.add(new AgentExecutionContext.HistoryEntry(
                    "user", "q" + g, null, null, g * 2 + 1, java.time.LocalDateTime.now(),
                    List.of(), null, null, null));
            history.add(new AgentExecutionContext.HistoryEntry(
                    "assistant", "a" + g, null, null, g * 2 + 2, java.time.LocalDateTime.now(),
                    List.of(), null, null, null));
        }
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "hello", null, null, groupCount * 2 - 1, java.time.LocalDateTime.now(),
                List.of(), null, null, null));
        return history;
    }

    @Test
    void fold_groupsWithinRecentCount_shouldNotFold() {
        List<AgentExecutionContext.HistoryEntry> history = buildFoldHistory(3);
        com.ghost616.agentbase.dto.model.ChatRequest captured = executeFoldChat(history, 3, null);

        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        assertFalse(contents.stream().anyMatch(c -> c != null && c.contains("此为历史消息索引为")),
                "组数不超过 recentCount 时不应折叠");
        assertTrue(contents.contains("q0") && contents.contains("a1"),
                "应保留全部历史消息内容");
    }

    @Test
    void fold_excessLessThanInterval_shouldNotFold() {
        List<AgentExecutionContext.HistoryEntry> history = buildFoldHistory(12);
        com.ghost616.agentbase.dto.model.ChatRequest captured = executeFoldChat(history, 3, null);

        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        assertFalse(contents.stream().anyMatch(c -> c != null && c.contains("此为历史消息索引为")),
                "超出 recentCount 但不足一个 interval 时不应折叠");
        assertTrue(contents.contains("a10"), "应保留全部历史消息内容");
    }

    @Test
    void fold_excessEqualToInterval_shouldFoldBatch() {
        List<AgentExecutionContext.HistoryEntry> history = buildFoldHistory(13);
        com.ghost616.agentbase.dto.model.ChatRequest captured = executeFoldChat(history, 3, null);

        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        long placeholderCount = contents.stream()
                .filter(c -> c != null && c.contains("此为历史消息索引为"))
                .count();
        assertEquals(10, placeholderCount, "超出恰好一个 interval 时应批量折叠 10 组");
        assertTrue(contents.contains("a10"), "近端区应完整保留");
        assertFalse(contents.contains("a9"), "折叠区应隐藏 assistant 内容");
    }

    @Test
    void fold_excessMultipleOfInterval_shouldFoldBatch() {
        List<AgentExecutionContext.HistoryEntry> history = buildFoldHistory(30);
        com.ghost616.agentbase.dto.model.ChatRequest captured = executeFoldChat(history, 5, null);

        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        long placeholderCount = contents.stream()
                .filter(c -> c != null && c.contains("此为历史消息索引为"))
                .count();
        assertEquals(20, placeholderCount, "超出多个 interval 时应批量折叠 20 组");
        assertTrue(contents.contains("a25"), "近端区应完整保留");
        assertFalse(contents.contains("a15"), "折叠区应隐藏 assistant 内容");
    }

    @Test
    void fold_anchorExpansionInsertedBeforeLastUser() {
        List<AgentExecutionContext.HistoryEntry> history = buildFoldHistory(13);
        com.ghost616.agentbase.dto.model.ChatRequest captured = executeFoldChat(history, 3, "[2]");

        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        String anchor = contents.stream()
                .filter(c -> c != null && c.startsWith("【历史消息组2】"))
                .findFirst().orElse(null);
        assertNotNull(anchor, "应生成组2的锚点展开消息");
        assertTrue(anchor.contains("\"role\":\"user\"") && anchor.contains("\"content\":\"q2\""),
                "锚点应包含 user 内容");
        assertTrue(anchor.contains("\"role\":\"assistant\"") && anchor.contains("\"content\":\"a2\""),
                "锚点应包含 assistant 内容");

        int anchorIdx = -1;
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i) != null && contents.get(i).startsWith("【历史消息组2】")) {
                anchorIdx = i;
                break;
            }
        }
        int lastUserIdx = contents.lastIndexOf("hello");
        assertTrue(anchorIdx >= 0 && anchorIdx < lastUserIdx,
                "锚点展开消息应插入在最后一条 user 消息之前");
    }

    @Test
    void fold_anchorExpansion包含工具调用推理与结果() {
        ToolCall toolCall = ToolCall.builder()
                .id("tc1")
                .name("get_weather")
                .arguments("{\"city\":\"sh\"}")
                .build();
        List<AgentExecutionContext.HistoryEntry> history = new ArrayList<>();
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "q0", null, null, 1, java.time.LocalDateTime.now(), List.of(), null, null, null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "assistant", "a0", null, null, 2, java.time.LocalDateTime.now(), List.of(), null, null, null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "q1", null, null, 3, java.time.LocalDateTime.now(), List.of(), null, null, null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "assistant", "a1", null, null, 4, java.time.LocalDateTime.now(), List.of(), null, null, null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "q2", null, null, 5, java.time.LocalDateTime.now(), List.of(), null, null, null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "assistant", "调用工具", "reasoning_text", null, 6, java.time.LocalDateTime.now(),
                List.of(toolCall), null, null, null));
        history.add(new AgentExecutionContext.HistoryEntry(
                "tool", "{\"temp\":25}", null, new ToolInfo("tc1", "get_weather"), 7, java.time.LocalDateTime.now(),
                List.of(), null, null, null));
        for (int g = 3; g < 13; g++) {
            history.add(new AgentExecutionContext.HistoryEntry(
                    "user", "q" + g, null, null, g * 2 + 1, java.time.LocalDateTime.now(), List.of(), null, null, null));
            history.add(new AgentExecutionContext.HistoryEntry(
                    "assistant", "a" + g, null, null, g * 2 + 2, java.time.LocalDateTime.now(), List.of(), null, null, null));
        }
        history.add(new AgentExecutionContext.HistoryEntry(
                "user", "hello", null, null, 27, java.time.LocalDateTime.now(), List.of(), null, null, null));

        com.ghost616.agentbase.dto.model.ChatRequest captured = executeFoldChat(history, 3, "[2]");

        List<String> contents = captured.getMessages().stream().map(Message::getContent).toList();
        String anchor = contents.stream()
                .filter(c -> c != null && c.startsWith("【历史消息组2】"))
                .findFirst().orElse(null);
        assertNotNull(anchor, "应生成组2锚点展开消息");
        assertTrue(anchor.contains("\"reasoning\":\"reasoning_text\""), "锚点应包含 assistant 推理内容");
        assertTrue(anchor.contains("\"tool_calls\"") && anchor.contains("\"name\":\"get_weather\"")
                && anchor.contains("city"), "锚点应包含工具名与参数");
        assertTrue(anchor.contains("\"tool_info\"") && anchor.contains("\"id\":\"tc1\"")
                && anchor.contains("temp"), "锚点应包含工具调用信息与结果");
    }
}
