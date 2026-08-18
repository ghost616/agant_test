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
    void bind_连接有效_绑定成功() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind("usr-1", session);

        assertTrue(result.success());
        assertNull(result.message());
        assertEquals(List.of(session), registry.getSessions("usr-1"));
    }

    @Test
    void bind_用户会话ID为空_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind("  ", session);

        assertFalse(result.success());
        assertEquals("用户会话 ID 无效", result.message());
        assertTrue(registry.getSessions("usr-1").isEmpty());
    }

    @Test
    void bind_连接为null_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind("usr-1", null);

        assertFalse(result.success());
        assertEquals("连接无效", result.message());
        assertTrue(registry.getSessions("usr-1").isEmpty());
    }

    @Test
    void unbind_解绑后连接移除() {
        SessionConnectionRegistry registry = newRegistry();
        registry.bind("usr-1", session);

        registry.unbind("usr-1", session);

        assertTrue(registry.getSessions("usr-1").isEmpty());
    }

    @Test
    void getSessions_多连接绑定同一用户会话() {
        WebSocketSession session2 = mock(WebSocketSession.class);
        SessionConnectionRegistry registry = newRegistry();

        registry.bind("usr-1", session);
        registry.bind("usr-1", session2);

        assertEquals(2, registry.getSessions("usr-1").size());
        assertTrue(registry.getSessions("usr-1").contains(session));
        assertTrue(registry.getSessions("usr-1").contains(session2));
    }

    @Test
    void getSessions_未绑定用户会话返回空列表() {
        SessionConnectionRegistry registry = newRegistry();

        assertTrue(registry.getSessions("usr-999").isEmpty());
    }

    @Test
    void getSessions_null用户会话ID返回空列表() {
        SessionConnectionRegistry registry = newRegistry();

        assertTrue(registry.getSessions(null).isEmpty());
        assertTrue(registry.getSessions("  ").isEmpty());
    }

    @Test
    void removeAll_连接关闭清理全部绑定() {
        SessionConnectionRegistry registry = newRegistry();
        registry.bind("usr-1", session);
        registry.bind("usr-2", session);

        registry.removeAll(session);

        assertTrue(registry.getSessions("usr-1").isEmpty());
        assertTrue(registry.getSessions("usr-2").isEmpty());
    }

    @Test
    void removeAll_未绑定连接不抛异常() {
        SessionConnectionRegistry registry = newRegistry();

        assertDoesNotThrow(() -> registry.removeAll(session));
        assertTrue(registry.getSessions("usr-1").isEmpty());
    }
}
