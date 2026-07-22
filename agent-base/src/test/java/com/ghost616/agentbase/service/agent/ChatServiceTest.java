package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.invoker.HookData;
import com.ghost616.agentbase.service.agent.invoker.HookManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

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

    private final Long sessionId = 1L;

    @BeforeEach
    void setUp() {
        registry = new AgentComponentRegistry();
        registry.setAgentContextManager(agentContextManager);
        registry.setSessionManager(sessionManager);
        registry.setModelInvokerManager(modelInvokerManager);
        registry.setSystemToolManager(systemToolManager);
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
        when(chatDataProvider.getModelConfig(any())).thenReturn(new ModelConfigData(1L, "key", "url", "model", 0.7, 4096, "openai"));
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
        when(chatDataProvider.getModelConfig(any())).thenReturn(new ModelConfigData(1L, "key", "url", "model", 0.7, 4096, "openai"));
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
        when(chatDataProvider.getModelConfig(any())).thenReturn(new ModelConfigData(1L, "key", "url", "model", 0.7, 4096, "openai"));
        when(modelInvokerManager.getInvoker(any())).thenReturn(modelInvoker);
        com.ghost616.agentbase.dto.model.ChatChunk chunk = com.ghost616.agentbase.dto.model.ChatChunk.builder().delta("hi").finishReason("stop").build();
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(chunk));

        chatService.chat(request).subscribe();

        com.ghost616.agentbase.dto.model.ChatChunk completeChunk = com.ghost616.agentbase.dto.model.ChatChunk.builder().hasToolCalls(false).build();
        verify(hookManager).triggerSessionHooks(sessionId, HookPhase.AFTER_MESSAGE_RECEIVE, context, new HookData(completeChunk));
        verify(hookManager).triggerHooks(HookPhase.AFTER_MESSAGE_RECEIVE, context, new HookData(completeChunk));
    }
}
