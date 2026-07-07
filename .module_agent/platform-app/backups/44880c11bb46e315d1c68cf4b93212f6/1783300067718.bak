package com.ghost616.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.service.agent.ChatService;
import com.ghost616.platform.service.agent.ToolExecutionTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.platform.dto.chat.ChatRequest;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.ToolCallQueueManager;
import com.ghost616.agentbase.service.agent.invoker.ToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;


@ExtendWith(MockitoExtension.class)
class ToolExecutionControllerTest {

    @Mock
    private ToolCallQueueManager toolCallQueueManager;
    @Mock
    private ToolManager toolManager;
    @Mock
    private SystemToolManager systemToolManager;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private ChatService chatService;
    @Mock
    private AgentContextManager agentContextManager;
    @Mock
    private ToolExecutionTracker toolExecutionTracker;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ToolExecutionController controller;

    private final Long sessionId = 1L;

    @Test
    void executeTools_stopped为true_清空队列追踪器返回status_empty() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData(
                "tc-1", "testTool", "{}");
        lenient().when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        lenient().when(toolCallQueueManager.poll(sessionId)).thenReturn(peekData);
        lenient().when(toolCallQueueManager.hasPending(sessionId)).thenReturn(false);
        lenient().when(toolManager.getInvoker(sessionId, "testTool")).thenReturn(mock(ToolInvoker.class));

        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(context.isStopped()).thenReturn(true);

        AgentContextManager.AgentSessionContext sessionCtx =
                mock(AgentContextManager.AgentSessionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        ApiResponse<Map<String, Object>> response = controller.executeTools(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("empty", response.getData().get("status"));
        assertEquals(false, response.getData().get("hasMore"));
        verify(toolCallQueueManager).clear(sessionId);
        verify(toolExecutionTracker).clear(sessionId);
    }

    @Test
    void executeTools_stopped为false_正常执行() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData(
                "tc-1", "testTool", "{}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        when(toolCallQueueManager.poll(sessionId)).thenReturn(peekData);
        when(toolCallQueueManager.hasPending(sessionId)).thenReturn(false);

        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(context.isStopped()).thenReturn(false);

        AgentContextManager.AgentSessionContext sessionCtx =
                mock(AgentContextManager.AgentSessionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        when(toolManager.getInvoker(sessionId, "testTool")).thenReturn(mock(ToolInvoker.class));

        ApiResponse<Map<String, Object>> response = controller.executeTools(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("executing", response.getData().get("status"));
        verify(toolCallQueueManager, never()).clear(sessionId);
        verify(toolExecutionTracker, never()).clear(sessionId);
    }

    @Test
    void continueChat_stopped为true_清空队列追踪器返回空Flux() {
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(null);

        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(context.isStopped()).thenReturn(true);

        AgentContextManager.AgentSessionContext sessionCtx =
                mock(AgentContextManager.AgentSessionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        Flux<ServerSentEvent<ChatChunk>> result = controller.continueChat(sessionId);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(toolCallQueueManager).clear(sessionId);
        verify(toolExecutionTracker).clear(sessionId);
    }

    @Test
    void continueChat_stopped为false_正常继续() {
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(null);

        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(context.isStopped()).thenReturn(false);

        AgentContextManager.AgentSessionContext sessionCtx =
                mock(AgentContextManager.AgentSessionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        when(toolExecutionTracker.getAndClearResults(sessionId)).thenReturn(new java.util.ArrayList<>());

        ChatChunk chunk = ChatChunk.builder().delta("continue").build();
        when(chatService.chat(any(ChatRequest.class)))
                .thenReturn(Flux.just(ServerSentEvent.builder(chunk).build()));

        Flux<ServerSentEvent<ChatChunk>> result = controller.continueChat(sessionId);

        StepVerifier.create(result)
                .expectNextMatches(sse -> "continue".equals(sse.data().getDelta()))
                .expectComplete()
                .verify();

        verify(toolCallQueueManager).clear(sessionId);
        verify(toolExecutionTracker).clear(sessionId);
    }
}
