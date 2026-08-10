package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.dto.chat.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentMessageProxyTest {

    private static final Pattern CONVERSATION_ID_PATTERN = Pattern.compile("^[0-9a-z_]+$");

    @Mock
    private ChatService chatService;
    @Mock
    private ToolExecutionService toolExecutionService;

    private AgentMessageProxy proxy;
    private final String sessionId = "1";
    private final String modelId = "100";

    @BeforeEach
    void setUp() {
        proxy = new AgentMessageProxy(chatService, toolExecutionService);
    }

    @Test
    void setChatDataCacheManager后字段应被设置() {
        ChatDataCacheManager manager = new ChatDataCacheManager(new ChatDataCacheProvider() {
            @Override
            public String createCache(String sessionId, String conversationId) {
                return null;
            }

            @Override
            public boolean cacheExists(String cacheId) {
                return false;
            }

            @Override
            public boolean cacheExists(String sessionId, String conversationId) {
                return false;
            }

            @Override
            public boolean isCacheDone(String cacheId) {
                return false;
            }

            @Override
            public String getCacheId(String sessionId, String conversationId) {
                return null;
            }

            @Override
            public int getMaxChunkIndex(String cacheId) {
                return -1;
            }

            @Override
            public void appendChunk(String cacheId, ChatChunk chunk) {
            }

            @Override
            public void removeCache(String cacheId) {
            }

            @Override
            public List<ChatChunk> getChunks(String cacheId, int startIndex, int endIndex) {
                return Collections.emptyList();
            }
        });
        proxy.setChatDataCacheManager(manager);
        assertEquals(manager, getPrivateField(proxy, "chatDataCacheManager"));
    }

    @Test
    void sendUserMessage_无工具调用时返回文本消息() {
        ServerSentEvent<ChatChunk> event = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().delta("Hello back").hasToolCalls(false).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(event));

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId, null);

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

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId, null);

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

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId, null);

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

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId, null);

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

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId, null);

        assertEquals("assistant", result.getRole());
        assertEquals("Done", result.getContent());
        verify(toolExecutionService, times(1)).executeTool(any());
    }

    @Test
    void sendUserMessage_events为null时返回空消息() {
        when(chatService.chat(any())).thenReturn(Flux.empty());

        Message result = proxy.sendUserMessage(sessionId, "Hi", modelId, null);

        assertEquals("assistant", result.getRole());
        assertEquals("", result.getContent());
    }

    @Test
    void sendUserMessageToSession_自动生成24位conversationId并透传() {
        ServerSentEvent<ChatChunk> event = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().delta("Reply").hasToolCalls(false).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(event));

        Message result = proxy.sendUserMessageToSession(sessionId, "Hi", modelId, true);

        assertEquals("assistant", result.getRole());
        assertEquals("Reply", result.getContent());

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatService).chat(captor.capture());
        ChatRequest request = captor.getValue();
        assertEquals(sessionId, request.getSessionId());
        assertEquals("Hi", request.getContent());
        assertEquals(modelId, request.getModelId());
        assertEquals(Boolean.TRUE, request.getThinking());
        assertNotNull(request.getConversationId());
        assertEquals(24, request.getConversationId().length());
        assertTrue(CONVERSATION_ID_PATTERN.matcher(request.getConversationId()).matches());
    }

    @Test
    void sendUserMessageToSession_每次调用生成不同conversationId() {
        ServerSentEvent<ChatChunk> event = ServerSentEvent.<ChatChunk>builder()
                .data(ChatChunk.builder().delta("Reply").hasToolCalls(false).build())
                .build();
        when(chatService.chat(any())).thenReturn(Flux.just(event));

        proxy.sendUserMessageToSession(sessionId, "Hi", modelId, null);
        proxy.sendUserMessageToSession(sessionId, "Hi", modelId, null);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatService, times(2)).chat(captor.capture());
        assertNotEquals(captor.getAllValues().get(0).getConversationId(),
                captor.getAllValues().get(1).getConversationId());
    }

    @Test
    void sendUserMessageToSession_工具正常执行后返回文本() {
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

        Message result = proxy.sendUserMessageToSession(sessionId, "Hi", modelId, null);

        assertEquals("assistant", result.getRole());
        assertEquals("Result text", result.getContent());
        verify(toolExecutionService).executeTool(any());
        verify(toolExecutionService).continueAfterTools(any());
    }

    private static Object getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
