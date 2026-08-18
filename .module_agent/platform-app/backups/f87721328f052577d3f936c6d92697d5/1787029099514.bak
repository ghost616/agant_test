package com.ghost616.platform.websocket;

import com.ghost616.platform.entity.User;
import com.ghost616.platform.session.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionWebSocketHandlerTest {

    @Mock
    private SessionConnectionRegistry registry;

    @Mock
    private WebSocketSession session;

    private SessionWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SessionWebSocketHandler(registry);
    }

    private void stubUserSession(String sessionId) {
        Map<String, Object> attributes = new HashMap<>();
        User user = new User();
        user.setId(42L);
        attributes.put(AuthHandshakeInterceptor.USER_SESSION_ATTR,
                new UserSession(sessionId, user, System.currentTimeMillis()));
        when(session.getAttributes()).thenReturn(attributes);
    }

    @Test
    void afterConnectionEstablished_有UserSession_自动绑定() {
        stubUserSession("sid-1");

        handler.afterConnectionEstablished(session);

        verify(registry).bind("sid-1", session);
    }

    @Test
    void afterConnectionEstablished_无UserSession_不绑定() {
        when(session.getAttributes()).thenReturn(new HashMap<>());

        handler.afterConnectionEstablished(session);

        verify(registry, never()).bind(any(), any());
    }

    @Test
    void handleMessage_文本消息被静默忽略() throws Exception {
        handler.handleMessage(session, new TextMessage("{\"type\":\"BIND\",\"sessionId\":\"100\"}"));

        verify(registry, never()).bind(any(), any());
        verify(registry, never()).unbind(any(), any());
    }

    @Test
    void afterConnectionClosed_清理连接注册() {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(registry).removeAll(session);
    }
}
