package com.ghost616.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionWebSocketHandlerTest {

    @Mock
    private SessionConnectionRegistry registry;

    @Mock
    private WebSocketSession session;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SessionWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SessionWebSocketHandler(registry, objectMapper);
    }

    private void stubUserSession() {
        Map<String, Object> attributes = new HashMap<>();
        User user = new User();
        user.setId(42L);
        attributes.put(AuthHandshakeInterceptor.USER_SESSION_ATTR,
                new UserSession("sid-1", user, System.currentTimeMillis()));
        when(session.getAttributes()).thenReturn(attributes);
    }

    @Test
    void handleMessage_BIND成功_发送成功回执() throws Exception {
        stubUserSession();
        when(registry.bind(42L, "100", session)).thenReturn(SessionConnectionRegistry.BindResult.ok());

        handler.handleMessage(session, new TextMessage("{\"type\":\"BIND\",\"sessionId\":\"100\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        var json = objectMapper.readTree(captor.getValue().getPayload());
        assertEquals(SessionWebSocketHandler.MESSAGE_NAME_BIND_RESULT, json.get("messageName").asText());
        assertEquals("100", json.get("sessionId").asText());
        assertTrue(json.get("success").asBoolean());
    }

    @Test
    void handleMessage_BIND失败_发送失败回执() throws Exception {
        stubUserSession();
        when(registry.bind(42L, "100", session))
                .thenReturn(SessionConnectionRegistry.BindResult.fail("会话不存在"));

        handler.handleMessage(session, new TextMessage("{\"type\":\"BIND\",\"sessionId\":\"100\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        var json = objectMapper.readTree(captor.getValue().getPayload());
        assertFalse(json.get("success").asBoolean());
        assertEquals("会话不存在", json.get("message").asText());
    }

    @Test
    void handleMessage_UNBIND_委托注册中心解绑() throws Exception {
        handler.handleMessage(session, new TextMessage("{\"type\":\"UNBIND\",\"sessionId\":\"100\"}"));

        verify(registry).unbind("100", session);
    }

    @Test
    void handleMessage_未知消息类型_不调用注册中心() throws Exception {
        handler.handleMessage(session, new TextMessage("{\"type\":\"PING\",\"sessionId\":\"100\"}"));

        verify(registry, never()).bind(any(), any(), any());
        verify(registry, never()).unbind(any(), any());
    }

    @Test
    void handleMessage_非法JSON_不抛异常() throws Exception {
        handler.handleMessage(session, new TextMessage("not-json"));

        verify(registry, never()).bind(any(), any(), any());
        verify(registry, never()).unbind(any(), any());
    }

    @Test
    void afterConnectionClosed_清理连接注册() {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(registry).removeAll(session);
    }
}