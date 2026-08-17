package com.ghost616.platform.websocket;

import com.ghost616.platform.entity.User;
import com.ghost616.platform.session.UserSession;
import com.ghost616.platform.session.UserSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHandshakeInterceptorTest {

    @Mock
    private UserSessionManager userSessionManager;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private ServerHttpRequest requestWithCookie(String cookie) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    @Test
    void beforeHandshake_有效Cookie_鉴权通过并写入属性() {
        User user = new User();
        user.setId(42L);
        UserSession userSession = new UserSession("sid-1", user, System.currentTimeMillis());
        when(userSessionManager.getSession("sid-1")).thenReturn(userSession);
        AuthHandshakeInterceptor interceptor = new AuthHandshakeInterceptor(userSessionManager);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                requestWithCookie("SESSION_ID=sid-1; other=1"), response, wsHandler, attributes);

        assertTrue(result);
        assertSame(userSession, attributes.get(AuthHandshakeInterceptor.USER_SESSION_ATTR));
    }

    @Test
    void beforeHandshake_Cookie中无SESSION_ID_鉴权失败() {
        AuthHandshakeInterceptor interceptor = new AuthHandshakeInterceptor(userSessionManager);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                requestWithCookie("other=1"), response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void beforeHandshake_无Cookie头_鉴权失败() {
        AuthHandshakeInterceptor interceptor = new AuthHandshakeInterceptor(userSessionManager);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                requestWithCookie(null), response, wsHandler, attributes);

        assertFalse(result);
    }

    @Test
    void beforeHandshake_会话不存在_鉴权失败() {
        when(userSessionManager.getSession("expired")).thenReturn(null);
        AuthHandshakeInterceptor interceptor = new AuthHandshakeInterceptor(userSessionManager);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                requestWithCookie("SESSION_ID=expired"), response, wsHandler, attributes);

        assertFalse(result);
    }
}