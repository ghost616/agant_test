package com.ghost616.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.AfterEach;
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

    private void stubUserContext(String userSessionId) {
        User user = new User();
        user.setId(42L);
        UserContext.set(new UserSession(userSessionId, user, System.currentTimeMillis()));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void pushToSession_广播路径_按用户推送全部连接() throws Exception {
        stubUserContext("usr-1");
        when(session.isOpen()).thenReturn(true);
        when(registry.getSessionsByUser(42L)).thenReturn(List.of(session));
        WebSocketPushService service = newService();

        service.pushToSession(Map.of("messageName", "SEND_USER_MESSAGE", "sessionId", "100"));

        verify(registry).getSessionsByUser(42L);
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String json = captor.getValue().getPayload();
        assertEquals("SEND_USER_MESSAGE", objectMapper.readTree(json).get("messageName").asText());
        assertEquals("100", objectMapper.readTree(json).get("sessionId").asText());
    }

    @Test
    void pushToSession_无连接时静默丢弃() throws Exception {
        stubUserContext("usr-1");
        when(registry.getSessionsByUser(42L)).thenReturn(List.of());
        WebSocketPushService service = newService();

        service.pushToSession(Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToSession_连接已关闭时丢弃() throws Exception {
        stubUserContext("usr-1");
        when(session.isOpen()).thenReturn(false);
        when(registry.getSessionsByUser(42L)).thenReturn(List.of(session));
        WebSocketPushService service = newService();

        service.pushToSession(Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToSession_发送异常时静默丢弃() throws Exception {
        stubUserContext("usr-1");
        when(session.isOpen()).thenReturn(true);
        when(registry.getSessionsByUser(42L)).thenReturn(List.of(session));
        doThrow(new IllegalStateException("send failed"))
                .when(session).sendMessage(any(TextMessage.class));
        WebSocketPushService service = newService();

        assertDoesNotThrow(() -> service.pushToSession(Map.of("messageName", "SEND_USER_MESSAGE")));
    }

    @Test
    void pushToSession_无UserContext_静默丢弃() throws Exception {
        WebSocketPushService service = newService();

        service.pushToSession(Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(registry, never()).getSessionsByUser(any());
        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToSession_user为null_静默丢弃() throws Exception {
        UserContext.set(new UserSession("usr-1", null, System.currentTimeMillis()));
        WebSocketPushService service = newService();

        service.pushToSession(Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(registry, never()).getSessionsByUser(any());
        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToSession_null负载直接忽略() {
        stubUserContext("usr-1");
        WebSocketPushService service = newService();

        service.pushToSession(null);

        verify(registry, never()).getSessionsByUser(any());
    }

    @Test
    void pushToSession_序列化失败时静默丢弃() throws Exception {
        stubUserContext("usr-1");
        when(registry.getSessionsByUser(42L)).thenReturn(List.of(session));
        Object circular = new Object() {
            @SuppressWarnings("unused")
            public final Object self = this;
        };
        WebSocketPushService service = newService();

        assertDoesNotThrow(() -> service.pushToSession(circular));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToUserSession_精准路径_按用户会话推送() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(registry.getSessions("usr-1")).thenReturn(List.of(session));
        WebSocketPushService service = newService();

        service.pushToUserSession("usr-1", Map.of("messageName", "SEND_USER_MESSAGE", "sessionId", "100"));

        verify(registry).getSessions("usr-1");
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String json = captor.getValue().getPayload();
        assertEquals("SEND_USER_MESSAGE", objectMapper.readTree(json).get("messageName").asText());
        assertEquals("100", objectMapper.readTree(json).get("sessionId").asText());
    }

    @Test
    void pushToUserSession_userSessionId无效_静默丢弃() throws Exception {
        WebSocketPushService service = newService();

        service.pushToUserSession("  ", Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(registry, never()).getSessions(any());
        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToUserSession_无连接时静默丢弃() throws Exception {
        when(registry.getSessions("usr-1")).thenReturn(List.of());
        WebSocketPushService service = newService();

        service.pushToUserSession("usr-1", Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToUserSession_连接已关闭时丢弃() throws Exception {
        when(session.isOpen()).thenReturn(false);
        when(registry.getSessions("usr-1")).thenReturn(List.of(session));
        WebSocketPushService service = newService();

        service.pushToUserSession("usr-1", Map.of("messageName", "SEND_USER_MESSAGE"));

        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToUserSession_发送异常时静默丢弃() throws Exception {
        when(session.isOpen()).thenReturn(true);
        when(registry.getSessions("usr-1")).thenReturn(List.of(session));
        doThrow(new IllegalStateException("send failed"))
                .when(session).sendMessage(any(TextMessage.class));
        WebSocketPushService service = newService();

        assertDoesNotThrow(() -> service.pushToUserSession("usr-1", Map.of("messageName", "SEND_USER_MESSAGE")));
    }

    @Test
    void pushToUserSession_序列化失败时静默丢弃() throws Exception {
        when(registry.getSessions("usr-1")).thenReturn(List.of(session));
        Object circular = new Object() {
            @SuppressWarnings("unused")
            public final Object self = this;
        };
        WebSocketPushService service = newService();

        assertDoesNotThrow(() -> service.pushToUserSession("usr-1", circular));
        verify(session, never()).sendMessage(any());
    }

    @Test
    void pushToUserSession_null负载直接忽略() {
        WebSocketPushService service = newService();

        service.pushToUserSession("usr-1", null);

        verify(registry, never()).getSessions(any());
    }
}
