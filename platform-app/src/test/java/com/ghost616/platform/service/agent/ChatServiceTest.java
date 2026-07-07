package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.agent.ChatService;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock
    private AgentContextManager agentContextManager;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private ModelConfigMapper modelConfigMapper;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private SystemToolManager systemToolManager;
    @Mock
    private ModelInvoker modelInvoker;

    @InjectMocks
    private ChatService chatService;

    private AgentExecutionContext context;
    private AgentExecutionContext.AgentContextMutator mutator;
    private AgentContextManager.AgentSessionContext sessionContext;

    @BeforeEach
    void setUp() {
        mutator = new AgentExecutionContext.AgentContextMutator();
        context = new AgentExecutionContext(
                1L, 1L, "system prompt", 1L, 10,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                mutator, new HashMap<>(), new HashMap<>());
        AtomicBoolean toolInvoking = new AtomicBoolean(false);
        sessionContext = new AgentContextManager.AgentSessionContext(context, mutator, toolInvoking);
        when(systemToolManager.getToolDefinitions()).thenReturn(java.util.Collections.emptyList());

        SessionManager.MessageSaveBuilder saveBuilder = mock(SessionManager.MessageSaveBuilder.class);
        when(sessionManager.save()).thenReturn(saveBuilder);
        when(saveBuilder.sessionId(any())).thenReturn(saveBuilder);
        when(saveBuilder.role(any())).thenReturn(saveBuilder);
        when(saveBuilder.content(any())).thenReturn(saveBuilder);
        when(saveBuilder.save()).thenReturn(null);
    }

    @Test
    void takeWhile_stopped为true时_非toolContinue消息会resetStopped_正常发射() {
        ChatRequest request = ChatRequest.builder()
                .sessionId(1L)
                .content("hello")
                .modelId(1L)
                .build();

        AgentContextManager.Builder builder = mock(AgentContextManager.Builder.class);
        when(agentContextManager.build(1L)).thenReturn(builder);
        when(builder.modelIdOverride(1L)).thenReturn(builder);
        when(builder.build()).thenReturn(sessionContext);
        when(sessionMapper.selectById(1L)).thenReturn(null);

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setId(1L);
        when(modelConfigMapper.selectById(1L)).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk1 = ChatChunk.builder().delta("chunk1").build();
        ChatChunk chunk2 = ChatChunk.builder().delta("chunk2").build();
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(chunk1, chunk2));

        mutator.setStopped();

        Flux<ServerSentEvent<ChatChunk>> result = chatService.chat(request);

        StepVerifier.create(result)
                .expectNextMatches(sse -> "chunk1".equals(sse.data().getDelta()))
                .expectNextMatches(sse -> "chunk2".equals(sse.data().getDelta()))
                .expectComplete()
                .verify();

        assertTrue(context.isStopped() == false);
    }

    @Test
    void takeWhile_stopped为false时正常发射() {
        ChatRequest request = ChatRequest.builder()
                .sessionId(1L)
                .content("hello")
                .modelId(1L)
                .build();

        AgentContextManager.Builder builder = mock(AgentContextManager.Builder.class);
        when(agentContextManager.build(1L)).thenReturn(builder);
        when(builder.modelIdOverride(1L)).thenReturn(builder);
        when(builder.build()).thenReturn(sessionContext);
        when(sessionMapper.selectById(1L)).thenReturn(null);

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setId(1L);
        when(modelConfigMapper.selectById(1L)).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk1 = ChatChunk.builder().delta("chunk1").build();
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(chunk1));

        Flux<ServerSentEvent<ChatChunk>> result = chatService.chat(request);

        StepVerifier.create(result)
                .expectNextMatches(sse -> {
                    ChatChunk data = sse.data();
                    return "chunk1".equals(data.getDelta());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void toolContinue路径不应调用resetStopped() {
        ChatRequest request = ChatRequest.builder()
                .sessionId(1L)
                .content(ChatService.TOOL_CONTINUE_MARKER)
                .modelId(1L)
                .build();

        AgentContextManager.Builder builder = mock(AgentContextManager.Builder.class);
        when(agentContextManager.build(1L)).thenReturn(builder);
        when(builder.modelIdOverride(1L)).thenReturn(builder);
        when(builder.build()).thenReturn(sessionContext);
        when(sessionMapper.selectById(1L)).thenReturn(null);

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setId(1L);
        when(modelConfigMapper.selectById(1L)).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk1 = ChatChunk.builder().delta("chunk1").build();
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(chunk1));

        mutator.setStopped();

        Flux<ServerSentEvent<ChatChunk>> result = chatService.chat(request);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        assertTrue(context.isStopped());
    }

    @Test
    void doOnCancel_调用setStopped() {
        ChatRequest request = ChatRequest.builder()
                .sessionId(1L)
                .content("hello")
                .modelId(1L)
                .build();

        AgentContextManager.Builder builder = mock(AgentContextManager.Builder.class);
        when(agentContextManager.build(1L)).thenReturn(builder);
        when(builder.modelIdOverride(1L)).thenReturn(builder);
        when(builder.build()).thenReturn(sessionContext);
        when(sessionMapper.selectById(1L)).thenReturn(null);

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.setId(1L);
        when(modelConfigMapper.selectById(1L)).thenReturn(modelConfig);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(modelInvoker);

        ChatChunk chunk1 = ChatChunk.builder().delta("chunk1").build();
        when(modelInvoker.invokeStream(any())).thenReturn(Flux.just(chunk1).delayElements(Duration.ofMillis(100)));

        Flux<ServerSentEvent<ChatChunk>> result = chatService.chat(request);

        StepVerifier.create(result)
                .thenCancel()
                .verify();

        assertTrue(context.isStopped());
    }
}
