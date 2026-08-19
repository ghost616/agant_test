package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.enums.SubSessionOpenMode;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubSessionWebSocketModeResolverTest {

    @Mock
    private SessionMapper sessionMapper;

    @Mock
    private AgentConfigMapper agentConfigMapper;

    private SubSessionWebSocketModeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SubSessionWebSocketModeResolver(sessionMapper, agentConfigMapper);
    }

    private Session createChildSession(Long id, Long parentSessionId) {
        Session s = new Session();
        s.setId(id);
        s.setParentSessionId(parentSessionId);
        s.setIsChild(true);
        return s;
    }

    private Session createMainSession(Long id, Long agentId) {
        Session s = new Session();
        s.setId(id);
        s.setParentSessionId(null);
        s.setAgentId(agentId);
        return s;
    }

    private AgentConfig createAgentConfig(SubSessionOpenMode mode) {
        AgentConfig c = new AgentConfig();
        c.setSubSessionOpenMode(mode);
        return c;
    }

    @Test
    @DisplayName("子会话沿父链解析到主会话且主会话 agent 为 WEBSOCKET 时返回 true")
    void websocketSubSession_shouldReturnTrue() {
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 1L));
        when(sessionMapper.selectById(1L)).thenReturn(createMainSession(1L, 10L));
        when(agentConfigMapper.selectById(10L)).thenReturn(createAgentConfig(SubSessionOpenMode.WEBSOCKET));

        assertTrue(resolver.isWebSocketSubSession("2"));
    }

    @Test
    @DisplayName("多层父链（子会话-中间会话-主会话）也能解析到主会话")
    void multiLevelParentChain_shouldResolveMainSession() {
        when(sessionMapper.selectById(3L)).thenReturn(createChildSession(3L, 2L));
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 1L));
        when(sessionMapper.selectById(1L)).thenReturn(createMainSession(1L, 10L));
        when(agentConfigMapper.selectById(10L)).thenReturn(createAgentConfig(SubSessionOpenMode.WEBSOCKET));

        assertTrue(resolver.isWebSocketSubSession("3"));
    }

    @Test
    @DisplayName("主会话 agent 为 TOOL_CALL 时返回 false")
    void toolCallMainSession_shouldReturnFalse() {
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 1L));
        when(sessionMapper.selectById(1L)).thenReturn(createMainSession(1L, 10L));
        when(agentConfigMapper.selectById(10L)).thenReturn(createAgentConfig(SubSessionOpenMode.TOOL_CALL));

        assertFalse(resolver.isWebSocketSubSession("2"));
    }

    @Test
    @DisplayName("主会话 agent 配置缺失时返回 false")
    void missingAgentConfig_shouldReturnFalse() {
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 1L));
        when(sessionMapper.selectById(1L)).thenReturn(createMainSession(1L, 10L));
        when(agentConfigMapper.selectById(10L)).thenReturn(null);

        assertFalse(resolver.isWebSocketSubSession("2"));
    }

    @Test
    @DisplayName("isChild=false 的非子会话返回 false")
    void nonChildSession_shouldReturnFalse() {
        Session s = new Session();
        s.setId(2L);
        s.setParentSessionId(1L);
        s.setIsChild(false);
        when(sessionMapper.selectById(2L)).thenReturn(s);

        assertFalse(resolver.isWebSocketSubSession("2"));
    }

    @Test
    @DisplayName("主会话（无父会话）返回 false")
    void mainSession_shouldReturnFalse() {
        when(sessionMapper.selectById(1L)).thenReturn(createMainSession(1L, 10L));

        assertFalse(resolver.isWebSocketSubSession("1"));
    }

    @Test
    @DisplayName("会话不存在返回 false")
    void missingSession_shouldReturnFalse() {
        when(sessionMapper.selectById(99L)).thenReturn(null);

        assertFalse(resolver.isWebSocketSubSession("99"));
    }

    @Test
    @DisplayName("父链中间会话缺失返回 false")
    void brokenParentChain_shouldReturnFalse() {
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 5L));
        when(sessionMapper.selectById(5L)).thenReturn(null);

        assertFalse(resolver.isWebSocketSubSession("2"));
    }

    @Test
    @DisplayName("父链成环返回 false")
    void cyclicParentChain_shouldReturnFalse() {
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 3L));
        when(sessionMapper.selectById(3L)).thenReturn(createChildSession(3L, 2L));

        assertFalse(resolver.isWebSocketSubSession("2"));
    }

    @Test
    @DisplayName("sessionId 为 null 或空白返回 false 且不查询")
    void blankSessionId_shouldReturnFalse() {
        assertFalse(resolver.isWebSocketSubSession(null));
        assertFalse(resolver.isWebSocketSubSession(""));
        verifyNoInteractions(sessionMapper);
    }

    @Test
    @DisplayName("解析结果缓存：第二次调用不重复查询")
    void cachedResult_shouldNotQueryAgain() {
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 1L));
        when(sessionMapper.selectById(1L)).thenReturn(createMainSession(1L, 10L));
        when(agentConfigMapper.selectById(10L)).thenReturn(createAgentConfig(SubSessionOpenMode.WEBSOCKET));

        assertTrue(resolver.isWebSocketSubSession("2"));
        assertTrue(resolver.isWebSocketSubSession("2"));

        verify(sessionMapper, times(1)).selectById(2L);
        verify(sessionMapper, times(2)).selectById(1L);
        verify(agentConfigMapper, times(1)).selectById(10L);
    }

    @Test
    @DisplayName("clearCache 后重新解析")
    void clearCache_shouldReResolve() {
        when(sessionMapper.selectById(2L)).thenReturn(createChildSession(2L, 1L));
        when(sessionMapper.selectById(1L)).thenReturn(createMainSession(1L, 10L));
        when(agentConfigMapper.selectById(10L)).thenReturn(createAgentConfig(SubSessionOpenMode.WEBSOCKET));

        assertTrue(resolver.isWebSocketSubSession("2"));
        resolver.clearCache();
        assertTrue(resolver.isWebSocketSubSession("2"));

        verify(sessionMapper, times(2)).selectById(2L);
    }
}