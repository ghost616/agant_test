package com.ghost616.platform.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionConnectionRegistryTest {

    @Mock
    private WebSocketSession session;

    private SessionConnectionRegistry newRegistry() {
        return new SessionConnectionRegistry();
    }

    @Test
    void bind_双维度绑定成功_三个索引同步更新() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "usr-1", session);

        assertTrue(result.success());
        assertNull(result.message());
        assertEquals(List.of(session), registry.getSessions("usr-1"));
        assertEquals(List.of(session), registry.getSessionsByUser(42L));
    }

    @Test
    void bind_userId为null_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(null, "usr-1", session);

        assertFalse(result.success());
        assertEquals("用户 ID 无效", result.message());
        assertTrue(registry.getSessions("usr-1").isEmpty());
        assertTrue(registry.getSessionsByUser(42L).isEmpty());
    }

    @Test
    void bind_userSessionId为空_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "  ", session);

        assertFalse(result.success());
        assertEquals("用户会话 ID 无效", result.message());
        assertTrue(registry.getSessions("usr-1").isEmpty());
        assertTrue(registry.getSessionsByUser(42L).isEmpty());
    }

    @Test
    void bind_连接为null_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "usr-1", null);

        assertFalse(result.success());
        assertEquals("连接无效", result.message());
        assertTrue(registry.getSessions("usr-1").isEmpty());
        assertTrue(registry.getSessionsByUser(42L).isEmpty());
    }

    @Test
    void getSessions_精准获取_同一用户会话多连接() {
        WebSocketSession session2 = mock(WebSocketSession.class);
        SessionConnectionRegistry registry = newRegistry();

        registry.bind(42L, "usr-1", session);
        registry.bind(42L, "usr-1", session2);

        assertEquals(2, registry.getSessions("usr-1").size());
        assertTrue(registry.getSessions("usr-1").contains(session));
        assertTrue(registry.getSessions("usr-1").contains(session2));
    }

    @Test
    void getSessionsByUser_广播获取_多用户会话多连接() {
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);
        WebSocketSession s3 = mock(WebSocketSession.class);
        WebSocketSession s4 = mock(WebSocketSession.class);
        SessionConnectionRegistry registry = newRegistry();

        registry.bind(42L, "usr-1", s1);
        registry.bind(42L, "usr-1", s2);
        registry.bind(42L, "usr-2", s3);
        registry.bind(43L, "usr-3", s4);

        List<WebSocketSession> user42 = registry.getSessionsByUser(42L);
        assertEquals(3, user42.size());
        assertTrue(user42.contains(s1));
        assertTrue(user42.contains(s2));
        assertTrue(user42.contains(s3));

        List<WebSocketSession> user43 = registry.getSessionsByUser(43L);
        assertEquals(1, user43.size());
        assertTrue(user43.contains(s4));
    }

    @Test
    void getSessionsByUser_多用户隔离_其他用户会话不影响() {
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "usr-1", session);

        assertTrue(registry.getSessions("usr-2").isEmpty());
        assertTrue(registry.getSessionsByUser(43L).isEmpty());
    }

    @Test
    void getSessions_未绑定用户会话返回空列表() {
        SessionConnectionRegistry registry = newRegistry();

        assertTrue(registry.getSessions("usr-999").isEmpty());
    }

    @Test
    void getSessions_null参数返回空列表() {
        SessionConnectionRegistry registry = newRegistry();

        assertTrue(registry.getSessions(null).isEmpty());
        assertTrue(registry.getSessions("  ").isEmpty());
    }

    @Test
    void getSessionsByUser_null参数返回空列表() {
        SessionConnectionRegistry registry = newRegistry();

        assertTrue(registry.getSessionsByUser(null).isEmpty());
    }

    @Test
    void unbind_三索引同步清理() {
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "usr-1", session);

        registry.unbind(42L, "usr-1", session);

        assertTrue(registry.getSessions("usr-1").isEmpty());
        assertTrue(registry.getSessionsByUser(42L).isEmpty());
    }

    @Test
    void unbind_该用户会话仍有其他连接_保留用户索引() {
        WebSocketSession session2 = mock(WebSocketSession.class);
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "usr-1", session);
        registry.bind(42L, "usr-1", session2);

        registry.unbind(42L, "usr-1", session);

        List<WebSocketSession> remaining = registry.getSessions("usr-1");
        assertEquals(1, remaining.size());
        assertTrue(remaining.contains(session2));
        assertEquals(1, registry.getSessionsByUser(42L).size());
    }

    @Test
    void unbind_null参数不抛异常() {
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "usr-1", session);

        assertDoesNotThrow(() -> registry.unbind(42L, null, session));
        assertDoesNotThrow(() -> registry.unbind(null, "usr-1", null));
        assertEquals(List.of(session), registry.getSessions("usr-1"));
    }

    @Test
    void removeAll_连接关闭清理全部绑定() {
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "usr-1", session);

        registry.removeAll(session);

        assertTrue(registry.getSessions("usr-1").isEmpty());
        assertTrue(registry.getSessionsByUser(42L).isEmpty());
    }

    @Test
    void removeAll_多用户会话连接逐一关闭_用户索引逐步清理() {
        WebSocketSession sessionA = mock(WebSocketSession.class);
        WebSocketSession sessionB = mock(WebSocketSession.class);
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "usr-1", sessionA);
        registry.bind(42L, "usr-2", sessionB);

        registry.removeAll(sessionA);

        assertTrue(registry.getSessions("usr-1").isEmpty());
        assertEquals(List.of(sessionB), registry.getSessionsByUser(42L));

        registry.removeAll(sessionB);

        assertTrue(registry.getSessions("usr-2").isEmpty());
        assertTrue(registry.getSessionsByUser(42L).isEmpty());
    }

    @Test
    void removeAll_未绑定连接不抛异常() {
        SessionConnectionRegistry registry = newRegistry();

        assertDoesNotThrow(() -> registry.removeAll(session));
        assertTrue(registry.getSessions("usr-1").isEmpty());
    }
}
