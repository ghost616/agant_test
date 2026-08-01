package com.ghost616.platform.controller;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolInfo;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.session.SubSessionDataDTO;
import com.ghost616.platform.service.agent.DefaultSubSessionCallback;
import com.ghost616.platform.service.session.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private DefaultSubSessionCallback subSessionCallback;

    @InjectMocks
    private SessionController controller;

    @Test
    void getSubSessionDataShouldMapThinkingField() {
        DefaultSubSessionCallback.SubSessionData data = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(data.getChildSessionId()).thenReturn(100L);
        when(data.getUserMessage()).thenReturn("hello");
        when(data.getThinking()).thenReturn(true);
        when(subSessionCallback.getSubSessionData(1L)).thenReturn(data);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(1L);

        assertTrue(response.isSuccess());
        SubSessionDataDTO dto = response.getData();
        assertNotNull(dto);
        assertEquals(100L, dto.getChildSessionId());
        assertEquals("hello", dto.getUserMessage());
        assertTrue(dto.getThinking());
    }

    @Test
    void getSubSessionDataShouldMapThinkingNull() {
        DefaultSubSessionCallback.SubSessionData data = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(data.getChildSessionId()).thenReturn(200L);
        when(data.getUserMessage()).thenReturn("test");
        when(data.getThinking()).thenReturn(null);
        when(subSessionCallback.getSubSessionData(2L)).thenReturn(data);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(2L);

        assertTrue(response.isSuccess());
        SubSessionDataDTO dto = response.getData();
        assertNull(dto.getThinking());
    }

    @Test
    void getSubSessionDataShouldMapThinkingFalse() {
        DefaultSubSessionCallback.SubSessionData data = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(data.getChildSessionId()).thenReturn(300L);
        when(data.getUserMessage()).thenReturn("no");
        when(data.getThinking()).thenReturn(false);
        when(subSessionCallback.getSubSessionData(3L)).thenReturn(data);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(3L);

        assertTrue(response.isSuccess());
        SubSessionDataDTO dto = response.getData();
        assertFalse(dto.getThinking());
    }

    @Test
    void getSubSessionDataShouldReturnNullWhenDataNotFound() {
        when(subSessionCallback.getSubSessionData(999L)).thenReturn(null);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(999L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    void completeSubSession_shouldPropagateToolInfoAndMapToolCalls() {
        CompletableFuture<Message> future = new CompletableFuture<>();
        DefaultSubSessionCallback.SubSessionData data =
                new DefaultSubSessionCallback.SubSessionData(100L, "hi", false, future);
        when(subSessionCallback.getSubSessionData(1L)).thenReturn(data);

        ToolInfo toolInfo = new ToolInfo("call-1", "getWeather");
        MessageDataProvider.MessageDTO assistantMsg = buildMessageDTO(
                "assistant", "response", "thinking", toolInfo, "tc1", "func1", "{}");
        when(sessionService.getMessages(100L)).thenReturn(List.of(
                buildMessageDTO("user", "hi", null, null, null, null, null),
                assistantMsg));

        ApiResponse<Void> response = controller.completeSubSession(1L);

        assertTrue(response.isSuccess());
        Message completed = future.join();
        assertNotNull(completed);
        assertEquals("assistant", completed.getRole());
        assertEquals("response", completed.getContent());
        assertEquals("thinking", completed.getReasoning());
        assertNotNull(completed.getToolInfo());
        assertEquals("call-1", completed.getToolInfo().toolCallId());
        assertEquals("getWeather", completed.getToolInfo().toolName());
        assertNotNull(completed.getToolCalls());
        assertEquals(1, completed.getToolCalls().size());
        assertEquals("tc1", completed.getToolCalls().get(0).getId());
        assertEquals("func1", completed.getToolCalls().get(0).getName());
        assertEquals("{}", completed.getToolCalls().get(0).getArguments());
    }

    @Test
    void completeSubSession_shouldCompleteWithNullToolInfo() {
        CompletableFuture<Message> future = new CompletableFuture<>();
        DefaultSubSessionCallback.SubSessionData data =
                new DefaultSubSessionCallback.SubSessionData(200L, "hi", false, future);
        when(subSessionCallback.getSubSessionData(1L)).thenReturn(data);

        when(sessionService.getMessages(200L)).thenReturn(List.of(
                buildMessageDTO("assistant", "plain", null, null, null, null, null)));

        ApiResponse<Void> response = controller.completeSubSession(1L);

        assertTrue(response.isSuccess());
        Message completed = future.join();
        assertNull(completed.getToolInfo());
        assertNull(completed.getToolCalls());
    }

    @Test
    void completeSubSession_shouldFailWhenDataNotFound() {
        when(subSessionCallback.getSubSessionData(999L)).thenReturn(null);

        ApiResponse<Void> response = controller.completeSubSession(999L);

        assertFalse(response.isSuccess());
        assertEquals("SESSION-004", response.getCode());
        verify(sessionService, never()).getMessages(anyLong());
    }

    @Test
    void completeSubSession_shouldFailWhenNoMessages() {
        CompletableFuture<Message> future = new CompletableFuture<>();
        DefaultSubSessionCallback.SubSessionData data =
                new DefaultSubSessionCallback.SubSessionData(300L, "hi", false, future);
        when(subSessionCallback.getSubSessionData(1L)).thenReturn(data);
        when(sessionService.getMessages(300L)).thenReturn(List.of());

        ApiResponse<Void> response = controller.completeSubSession(1L);

        assertFalse(response.isSuccess());
        assertEquals("SESSION-005", response.getCode());
    }

    @Test
    void completeSubSession_shouldFailWhenNoAssistantMessage() {
        CompletableFuture<Message> future = new CompletableFuture<>();
        DefaultSubSessionCallback.SubSessionData data =
                new DefaultSubSessionCallback.SubSessionData(400L, "hi", false, future);
        when(subSessionCallback.getSubSessionData(1L)).thenReturn(data);
        when(sessionService.getMessages(400L)).thenReturn(List.of(
                buildMessageDTO("user", "hi", null, null, null, null, null)));

        ApiResponse<Void> response = controller.completeSubSession(1L);

        assertFalse(response.isSuccess());
        assertEquals("SESSION-005", response.getCode());
    }

    private MessageDataProvider.MessageDTO buildMessageDTO(String role, String content, String reasoning,
                                                           ToolInfo toolInfo, String tcId,
                                                           String tcName, String tcArgs) {
        List<MessageDataProvider.ToolCallData> toolCalls = tcId == null ? null
                : List.of(new MessageDataProvider.ToolCallData(tcId, tcName, tcArgs));
        return new MessageDataProvider.MessageDTO("1", "100", role, content, reasoning,
                toolInfo, 1, LocalDateTime.now(), null, toolCalls, null, false, null, null);
    }
}
