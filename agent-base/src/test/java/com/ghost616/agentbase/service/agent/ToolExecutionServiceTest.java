package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.ToolCallQueueManager;
import com.ghost616.agentbase.service.agent.invoker.ToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.util.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolExecutionServiceTest {

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

    private ToolExecutionService toolExecutionService;

    private final Long sessionId = 1L;

    @BeforeEach
    void setUp() {
        toolExecutionService = new ToolExecutionService(
                toolCallQueueManager, toolManager, systemToolManager,
                sessionManager, chatService, agentContextManager,
                toolExecutionTracker);
    }

    // ========== executeTool ==========

    @Test
    void executeTool_队列为空时返回empty() {
        when(toolCallQueueManager.peek(sessionId)).thenReturn(null);

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        assertEquals("empty", result.status());
        assertNull(result.toolId());
        assertNull(result.toolName());
    }

    @Test
    void executeTool_工具名以_sys_开头时调用SystemToolManager() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData("tid1", "_sys_getWeather", "{\"loc\":\"Beijing\"}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        SystemTool sysInvoker = mock(SystemTool.class);
        when(systemToolManager.getSystemTool("getWeather")).thenReturn(sysInvoker);

        MessageDataProvider.ToolCallData pollData = new MessageDataProvider.ToolCallData("tid1", "_sys_getWeather", "{\"loc\":\"Beijing\"}");
        when(toolCallQueueManager.poll(sessionId)).thenReturn(pollData);
        when(toolCallQueueManager.hasPending(sessionId)).thenReturn(false);
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(context.isStopped()).thenReturn(false);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        verify(systemToolManager).getSystemTool("getWeather");
        assertEquals("executing", result.status());
        assertEquals("tid1", result.toolId());
    }

    @Test
    void executeTool_非_sys_前缀时调用ToolManager() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData("tid2", "myTool", "{}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        ToolInvoker invoker = mock(ToolInvoker.class);
        when(toolManager.getInvoker(sessionId, "myTool")).thenReturn(invoker);

        MessageDataProvider.ToolCallData pollData = new MessageDataProvider.ToolCallData("tid2", "myTool", "{}");
        when(toolCallQueueManager.poll(sessionId)).thenReturn(pollData);
        when(toolCallQueueManager.hasPending(sessionId)).thenReturn(false);
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(context.isStopped()).thenReturn(false);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        verify(toolManager).getInvoker(sessionId, "myTool");
        assertEquals("executing", result.status());
    }

    @Test
    void executeTool_invoker为null时返回failed() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData("tid3", "unknownTool", "{}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        when(toolManager.getInvoker(sessionId, "unknownTool")).thenReturn(null);

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        assertEquals("failed", result.status());
        assertEquals("tid3", result.toolId());
        assertEquals("工具调用器不存在", result.message());
    }

    @Test
    void executeTool_获取调用器抛出异常时返回failed() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData("tid4", "_sys_broken", "{}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        when(systemToolManager.getSystemTool("broken")).thenThrow(new RuntimeException("connection error"));

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        assertEquals("failed", result.status());
        assertEquals("tid4", result.toolId());
        assertEquals("connection error", result.message());
    }

    @Test
    void executeTool_poll后sessionContext为null时返回error() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData("tid5", "myTool", "{}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        ToolInvoker invoker = mock(ToolInvoker.class);
        when(toolManager.getInvoker(sessionId, "myTool")).thenReturn(invoker);

        MessageDataProvider.ToolCallData pollData = new MessageDataProvider.ToolCallData("tid5", "myTool", "{}");
        when(toolCallQueueManager.poll(sessionId)).thenReturn(pollData);
        when(agentContextManager.get(sessionId)).thenReturn(null);

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        assertEquals("error", result.status());
        assertEquals("session not found", result.message());
    }

    @Test
    void executeTool_contextStopped时清理并返回empty() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData("tid6", "myTool", "{}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        ToolInvoker invoker = mock(ToolInvoker.class);
        when(toolManager.getInvoker(sessionId, "myTool")).thenReturn(invoker);

        MessageDataProvider.ToolCallData pollData = new MessageDataProvider.ToolCallData("tid6", "myTool", "{}");
        when(toolCallQueueManager.poll(sessionId)).thenReturn(pollData);
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(context.isStopped()).thenReturn(true);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        verify(toolCallQueueManager).clear(sessionId);
        verify(toolExecutionTracker).clear(sessionId);
        assertEquals("empty", result.status());
    }

    @Test
    void executeTool_正常流程返回executing() {
        MessageDataProvider.ToolCallData peekData = new MessageDataProvider.ToolCallData("tid7", "normalTool", "{\"key\":\"val\"}");
        when(toolCallQueueManager.peek(sessionId)).thenReturn(peekData);
        ToolInvoker invoker = mock(ToolInvoker.class);
        when(toolManager.getInvoker(sessionId, "normalTool")).thenReturn(invoker);

        MessageDataProvider.ToolCallData pollData = new MessageDataProvider.ToolCallData("tid7", "normalTool", "{\"key\":\"val\"}");
        when(toolCallQueueManager.poll(sessionId)).thenReturn(pollData);
        when(toolCallQueueManager.hasPending(sessionId)).thenReturn(true);
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(context.isStopped()).thenReturn(false);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);

        verify(toolExecutionTracker).setExecuting(sessionId, "tid7", "normalTool", "{\"key\":\"val\"}", true);
        assertEquals("executing", result.status());
        assertEquals("tid7", result.toolId());
        assertEquals("normalTool", result.toolName());
        assertEquals("{\"key\":\"val\"}", result.arguments());
        assertTrue(result.hasMore());
    }

    // ========== getToolStatus ==========

    @Test
    void getToolStatus_无执行中状态时返回idle() {
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(null);

        ToolExecutionService.ToolStatusResult result = toolExecutionService.getToolStatus(sessionId);

        assertEquals("idle", result.status());
        assertNull(result.toolId());
        assertNull(result.toolName());
    }

    @Test
    void getToolStatus_有执行中状态时返回对应数据() {
        ToolExecutionTracker.ToolExecutionStatus status = new ToolExecutionTracker.ToolExecutionStatus(
                "tid8", "runningTool", "{}", "executing", null, false);
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(status);

        ToolExecutionService.ToolStatusResult result = toolExecutionService.getToolStatus(sessionId);

        assertEquals("executing", result.status());
        assertEquals("tid8", result.toolId());
        assertEquals("runningTool", result.toolName());
        assertEquals("{}", result.arguments());
        assertFalse(result.hasMore());
        assertNull(result.result());
    }

    // ========== continueAfterTools ==========

    @Test
    void continueAfterTools_工具正在执行时抛出BusinessException() {
        ToolExecutionTracker.ToolExecutionStatus status = new ToolExecutionTracker.ToolExecutionStatus(
                "tid9", "tool", "{}", "executing", null, false);
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(status);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> toolExecutionService.continueAfterTools(sessionId));
        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    }

    @Test
    void continueAfterTools_sessionStopped时清理并返回FluxEmpty() {
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(null);
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(context.isStopped()).thenReturn(true);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        Flux<ServerSentEvent<ChatChunk>> result = toolExecutionService.continueAfterTools(sessionId);

        verify(toolCallQueueManager).clear(sessionId);
        verify(toolExecutionTracker).clear(sessionId);
        assertSame(Flux.empty(), result);
    }

    @Test
    void continueAfterTools_正常流程() throws Exception {
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(null);
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(context.isStopped()).thenReturn(false);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        ToolExecutionTracker.ToolResult toolResult = new ToolExecutionTracker.ToolResult(
                "tid10", "doneTool", "{}", "result_ok");
        when(toolExecutionTracker.getAndClearResults(sessionId)).thenReturn(List.of(toolResult));

        SessionManager.MessageSaveBuilder saveBuilder = mock(SessionManager.MessageSaveBuilder.class);
        when(saveBuilder.sessionId(any())).thenReturn(saveBuilder);
        when(saveBuilder.role(any())).thenReturn(saveBuilder);
        when(saveBuilder.content(any())).thenReturn(saveBuilder);
        when(saveBuilder.toolCallId(any())).thenReturn(saveBuilder);
        when(saveBuilder.toolResult(any())).thenReturn(saveBuilder);
        when(saveBuilder.save()).thenReturn(100L);
        when(sessionManager.save()).thenReturn(saveBuilder);

        Flux<ServerSentEvent<ChatChunk>> expectedFlux = Flux.empty();
        when(chatService.chat(any())).thenReturn(expectedFlux);

        Flux<ServerSentEvent<ChatChunk>> result = toolExecutionService.continueAfterTools(sessionId);

        verify(toolExecutionTracker).getAndClearResults(sessionId);
        verify(saveBuilder).sessionId(sessionId);
        verify(saveBuilder).role("tool");
        verify(saveBuilder).content("result_ok");
        verify(saveBuilder).toolCallId("tid10");
        ArgumentCaptor<String> toolResultCaptor = ArgumentCaptor.forClass(String.class);
        verify(saveBuilder).toolResult(toolResultCaptor.capture());
        String capturedResult = toolResultCaptor.getValue();
        assertTrue(capturedResult.contains("doneTool"));
        assertTrue(capturedResult.contains("result_ok"));
        verify(agentContextManager).addHistoryEntry(eq(sessionId), any(AgentExecutionContext.HistoryEntry.class));
        verify(toolCallQueueManager).clear(sessionId);
        verify(toolExecutionTracker).clear(sessionId);
        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatService).chat(requestCaptor.capture());
        assertEquals(sessionId, requestCaptor.getValue().getSessionId());
        assertEquals(ChatService.TOOL_CONTINUE_MARKER, requestCaptor.getValue().getContent());
        assertSame(expectedFlux, result);
    }

    @Test
    void continueAfterTools_无结果时跳过持久化和历史记录() {
        when(toolExecutionTracker.getCurrentExecution(sessionId)).thenReturn(null);
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext context = mock(AgentExecutionContext.class);
        when(sessionCtx.context()).thenReturn(context);
        when(context.isStopped()).thenReturn(false);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);

        when(toolExecutionTracker.getAndClearResults(sessionId)).thenReturn(List.of());

        Flux<ServerSentEvent<ChatChunk>> expectedFlux = Flux.empty();
        when(chatService.chat(any())).thenReturn(expectedFlux);

        toolExecutionService.continueAfterTools(sessionId);

        verify(sessionManager, never()).save();
        verify(agentContextManager, never()).addHistoryEntry(any(), any());
    }
}
