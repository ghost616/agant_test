package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentMessageProxyTest {

    @Mock
    private ChatService chatService;
    @Mock
    private ToolExecutionService toolExecutionService;

    private AgentMessageProxy proxy;
    private final Long sessionId = 1L;
    private final Long modelId = 100L;

    @BeforeEach
    void setUp() {
        proxy = new AgentMessageProxy(chatService, toolExecutionService);
    }

    @Test
    void sendUserMessage_无工具调用时返回文本消息() {
        ServerSentEvent<ChatChunk> event = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().delta("Hello back").hasToolCalls(false).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(event));

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId);

        assertEquals("assistant", result.getRole());
        assertEquals("Hello back", result.getContent());
        verify(toolExecutionService, never()).executeTool(any());
    }

    @Test
    void sendUserMessage_工具正常执行后返回文本() {
        ServerSentEvent<ChatChunk> toolEvent = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().hasToolCalls(true).build())
                .build();
        ServerSentEvent<ChatChunk> textEvent = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().delta("Result text").hasToolCalls(false).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(toolEvent));
        when(toolExecutionService.continueAfterTools(any())).thenReturn(Flux.just(textEvent));
        ToolExecutionService.ToolExecutionResult execResult = new ToolExecutionService.ToolExecutionResult(
                "executing", "tid1", "myTool", "{}", false, null);
        when(toolExecutionService.executeTool(any())).thenReturn(execResult);
        ToolExecutionService.ToolStatusResult statusResult = new ToolExecutionService.ToolStatusResult(
                "done", "tid1", "myTool", "{}", false, null);
        when(toolExecutionService.getToolStatus(any(), any())).thenReturn(statusResult);

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId);

        assertEquals("assistant", result.getRole());
        assertEquals("Result text", result.getContent());
        verify(toolExecutionService).executeTool(any());
        verify(toolExecutionService).continueAfterTools(any());
    }

    @Test
    void sendUserMessage_同一参数组合调用5次触发振荡保护() {
        ServerSentEvent<ChatChunk> toolEvent = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().hasToolCalls(true).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(toolEvent));
        ToolExecutionService.ToolExecutionResult execResult = new ToolExecutionService.ToolExecutionResult(
                "executing", "tid1", "repeatedTool", "{\"x\":1}", true, null);
        when(toolExecutionService.executeTool(any())).thenReturn(execResult);
        ToolExecutionService.ToolStatusResult statusResult = new ToolExecutionService.ToolStatusResult(
                "done", "tid1", "repeatedTool", "{\"x\":1}", false, null);
        when(toolExecutionService.getToolStatus(any(), any())).thenReturn(statusResult);

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId);

        assertEquals("assistant", result.getRole());
        assertEquals("", result.getContent());
        verify(toolExecutionService, times(5)).executeTool(any());
        verify(toolExecutionService, never()).continueAfterTools(any());
    }

    @Test
    void sendUserMessage_同一参数组合调用4次不触发振荡保护() {
        ServerSentEvent<ChatChunk> toolEvent = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().hasToolCalls(true).build())
                .build();
        ServerSentEvent<ChatChunk> textEvent = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().delta("OK").hasToolCalls(false).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(toolEvent));
        when(toolExecutionService.continueAfterTools(any())).thenReturn(Flux.just(textEvent));
        ToolExecutionService.ToolExecutionResult execResult = new ToolExecutionService.ToolExecutionResult(
                "executing", "tid1", "safeTool", "{}", false, null);
        when(toolExecutionService.executeTool(any())).thenReturn(execResult);
        ToolExecutionService.ToolStatusResult statusResult = new ToolExecutionService.ToolStatusResult(
                "done", "tid1", "safeTool", "{}", false, null);
        when(toolExecutionService.getToolStatus(any(), any())).thenReturn(statusResult);

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId);

        assertEquals("assistant", result.getRole());
        assertEquals("OK", result.getContent());
        verify(toolExecutionService, times(1)).executeTool(any());
    }

    @Test
    void sendUserMessage_工具执行返回empty时正常退出循环() {
        ServerSentEvent<ChatChunk> toolEvent = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().hasToolCalls(true).build())
                .build();
        ServerSentEvent<ChatChunk> textEvent = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().delta("Done").hasToolCalls(false).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(toolEvent));
        when(toolExecutionService.continueAfterTools(any())).thenReturn(Flux.just(textEvent));
        ToolExecutionService.ToolExecutionResult emptyResult = new ToolExecutionService.ToolExecutionResult(
                "empty", null, null, null, false, null);
        when(toolExecutionService.executeTool(any())).thenReturn(emptyResult);

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId);

        assertEquals("assistant", result.getRole());
        assertEquals("Done", result.getContent());
        verify(toolExecutionService, times(1)).executeTool(any());
    }

    @Test
    void sendUserMessage_events为null时返回空消息() {
        when(chatService.chat(any())).thenReturn(Flux.empty());

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId);

        assertEquals("assistant", result.getRole());
        assertEquals("", result.getContent());
    }
}
