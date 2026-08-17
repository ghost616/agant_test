package com.ghost616.platform.websocket;

import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.SessionMapper;
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
    private SessionMapper sessionMapper;

    @Mock
    private WebSocketSession session;

    private SessionConnectionRegistry newRegistry() {
        return new SessionConnectionRegistry(sessionMapper);
    }

    private Session sessionEntity(Long id, Long userId) {
        Session s = new Session();
        s.setId(id);
        s.setUserId(userId);
        return s;
    }

    @Test
    void bind_会话存在且属主匹配_绑定成功() {
        when(sessionMapper.selectById(100L)).thenReturn(sessionEntity(100L, 42L));
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "100", session);

        assertTrue(result.success());
        assertNull(result.message());
        assertEquals(List.of(session), registry.getSessions("100"));
    }

    @Test
    void bind_会话不存在_绑定失败() {
        when(sessionMapper.selectById(100L)).thenReturn(null);
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "100", session);

        assertFalse(result.success());
        assertEquals("会话不存在", result.message());
        assertTrue(registry.getSessions("100").isEmpty());
    }

    @Test
    void bind_非本人会话_绑定失败() {
        when(sessionMapper.selectById(100L)).thenReturn(sessionEntity(100L, 42L));
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(99L, "100", session);

        assertFalse(result.success());
        assertEquals("无权绑定该会话", result.message());
    }

    @Test
    void bind_会话ID非数字_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "abc", session);

        assertFalse(result.success());
        assertEquals("会话 ID 无效", result.message());
        verify(sessionMapper, never()).selectById(any());
    }

    @Test
    void bind_会话ID为空_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "  ", session);

        assertFalse(result.success());
        assertEquals("会话 ID 无效", result.message());
    }

    @Test
    void bind_连接为null_绑定失败() {
        SessionConnectionRegistry registry = newRegistry();

        SessionConnectionRegistry.BindResult result = registry.bind(42L, "100", null);

        assertFalse(result.success());
        verify(sessionMapper, never()).selectById(any());
    }

    @Test
    void unbind_解绑后连接移除() {
        when(sessionMapper.selectById(100L)).thenReturn(sessionEntity(100L, 42L));
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "100", session);

        registry.unbind("100", session);

        assertTrue(registry.getSessions("100").isEmpty());
    }

    @Test
    void getSessions_多连接绑定同一会话() {
        when(sessionMapper.selectById(100L)).thenReturn(sessionEntity(100L, 42L));
        WebSocketSession session2 = mock(WebSocketSession.class);
        SessionConnectionRegistry registry = newRegistry();

        registry.bind(42L, "100", session);
        registry.bind(42L, "100", session2);

        assertEquals(2, registry.getSessions("100").size());
        assertTrue(registry.getSessions("100").contains(session));
        assertTrue(registry.getSessions("100").contains(session2));
    }

    @Test
    void getSessions_未绑定会话返回空列表() {
        SessionConnectionRegistry registry = newRegistry();

        assertTrue(registry.getSessions("999").isEmpty());
    }

    @Test
    void removeAll_连接关闭清理全部绑定() {
        when(sessionMapper.selectById(100L)).thenReturn(sessionEntity(100L, 42L));
        when(sessionMapper.selectById(200L)).thenReturn(sessionEntity(200L, 42L));
        SessionConnectionRegistry registry = newRegistry();
        registry.bind(42L, "100", session);
        registry.bind(42L, "200", session);

        registry.removeAll(session);

        assertTrue(registry.getSessions("100").isEmpty());
        assertTrue(registry.getSessions("200").isEmpty());
    }

    @Test
    void removeAll_未绑定连接不抛异常() {
        SessionConnectionRegistry registry = newRegistry();

        assertDoesNotThrow(() -> registry.removeAll(session));
        assertTrue(registry.getSessions("100").isEmpty());
    }
}