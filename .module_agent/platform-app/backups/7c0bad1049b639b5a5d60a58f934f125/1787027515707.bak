package com.ghost616.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketPushServiceTest {

    @Mock
    private SessionConnectionRegistry registry;

    @Mock
    private WebSocketSession session;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketPushService newService() {
        return new WebSocketPushService(registry, objectMapper);
    }

    @Test
    void pushToSession_有连接时序列化并推送JSON() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(registry.getSessions("100")).thenReturn(List.of(session));
        WebSocketPushService service = newService();

        service.pushToSession("100", Map.of("messageName", "SEND_USER_MESSAGE", "sessionId", "100"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String json = captor.getValue().getPayload();
        assertEquals("SEND_USER_MESSAGE", objectMapper.readTree(json).get("messageName").asText());
        assertEquals("100", objectMapper.readTree(json).get("sessionId").asText());
    }

    @Test
    void pushToSession_无连接时静默丢弃() throws Exception {
        when(registry.getSessions("100")).thenReturn(List.of());
        WebSocketPushService service = newService();

        service.pushToSession("100", Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToSession_连接已关闭时丢弃() throws Exception {
        when(session.isOpen()).thenReturn(false);
        when(registry.getSessions("100")).thenReturn(List.of(session));
        WebSocketPushService service = newService();

        service.pushToSession("100", Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToSession_发送异常时静默丢弃() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(registry.getSessions("100")).thenReturn(List.of(session));
        doThrow(new IllegalStateException("send failed"))
                .when(session).sendMessage(any(TextMessage.class));
        WebSocketPushService service = newService();

        assertDoesNotThrow(() -> service.pushToSession("100", Map.of("messageName", "SEND_USER_MESSAGE")));
    }

    @Test
    void pushToSession_空会话ID直接忽略() {
        WebSocketPushService service = newService();

        service.pushToSession("  ", Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(registry, never()).getSessions(any());
    }

    @Test
    void pushToSession_null负载直接忽略() {
        WebSocketPushService service = newService();

        service.pushToSession("100", null);

        verify(registry, never()).getSessions(any());
    }

    @Test
    void pushToSession_序列化失败时静默丢弃() throws Exception {
        when(registry.getSessions("100")).thenReturn(List.of(session));
        Object circular = new Object() {
            @SuppressWarnings("unused")
            public final Object self = this;
        };
        WebSocketPushService service = newService();

        assertDoesNotThrow(() -> service.pushToSession("100", circular));
        verify(session, never()).sendMessage(any());
    }
}