package com.ghost616.platform.session;

import com.ghost616.platform.dto.session.SessionDTO;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.AgentToolMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import com.ghost616.platform.service.session.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;


@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private AgentToolMapper agentToolMapper;
    @Mock
    private SessionToolMapper sessionToolMapper;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private AgentContextManager agentContextManager;
    @Mock
    private ToolManager toolManager;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private Session parentSession;
    private Session childSession1;
    private Session childSession2;
    private Session nonChildSession;
    private final Long parentId = 10L;

    @BeforeEach
    void setUp() {
        parentSession = new Session();
        parentSession.setId(1L);
        parentSession.setTitle("parent");
        parentSession.setIsChild(false);

        childSession1 = new Session();
        childSession1.setId(2L);
        childSession1.setParentSessionId(parentId);
        childSession1.setIsChild(true);
        childSession1.setTitle("child1");
        childSession1.setDescription("first child");
        childSession1.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));

        childSession2 = new Session();
        childSession2.setId(3L);
        childSession2.setParentSessionId(parentId);
        childSession2.setIsChild(true);
        childSession2.setTitle("child2");
        childSession2.setDescription("second child");
        childSession2.setCreateTime(LocalDateTime.of(2026, 1, 2, 0, 0));

        nonChildSession = new Session();
        nonChildSession.setId(4L);
        nonChildSession.setParentSessionId(parentId);
        nonChildSession.setIsChild(false);
        nonChildSession.setTitle("non-child");
    }

    @Test
    void listChildSessions_有子会话_返回DTO列表() {
        when(sessionMapper.selectList(any())).thenReturn(List.of(childSession1, childSession2));

        List<SessionDTO> result = sessionService.listChildSessions(parentId);

        assertEquals(2, result.size());
        assertEquals("child1", result.get(0).getTitle());
        assertEquals("first child", result.get(0).getDescription());
        assertTrue(result.get(0).getIsChild());
        assertEquals(parentId, result.get(0).getParentSessionId());

        assertEquals("child2", result.get(1).getTitle());
    }

    @Test
    void listChildSessions_无子会话_返回空列表() {
        when(sessionMapper.selectList(any())).thenReturn(List.of());

        List<SessionDTO> result = sessionService.listChildSessions(parentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void listChildSessions_查询条件包含parentId和isChild() {
        when(sessionMapper.selectList(any())).thenReturn(List.of(childSession1));

        List<SessionDTO> result = sessionService.listChildSessions(parentId);

        assertEquals(1, result.size());
        verify(sessionMapper).selectList(any());
    }

    @Test
    void listChildSessions_按创建时间倒序() {
        when(sessionMapper.selectList(any())).thenReturn(List.of(childSession2, childSession1));

        List<SessionDTO> result = sessionService.listChildSessions(parentId);

        assertEquals(2, result.size());
        assertEquals("child2", result.get(0).getTitle());
        assertEquals("child1", result.get(1).getTitle());
    }

    @Test
    void listChildSessions_非子会话不返回() {
        when(sessionMapper.selectList(any())).thenReturn(List.of());

        List<SessionDTO> result = sessionService.listChildSessions(parentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void toDTO_映射新字段() {
        Session entity = new Session();
        entity.setId(100L);
        entity.setAgentId(200L);
        entity.setModelId(300L);
        entity.setTitle("test-title");
        entity.setSystemPrompt("test-prompt");
        entity.setParentSessionId(50L);
        entity.setIsChild(true);
        entity.setDescription("test-description");
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        when(sessionMapper.selectList(any())).thenReturn(List.of(entity));

        List<SessionDTO> result = sessionService.listChildSessions(50L);

        assertEquals(1, result.size());
        SessionDTO dto = result.get(0);
        assertEquals(100L, dto.getId());
        assertEquals(200L, dto.getAgentId());
        assertEquals(300L, dto.getModelId());
        assertEquals("test-title", dto.getTitle());
        assertEquals("test-prompt", dto.getSystemPrompt());
        assertEquals(50L, dto.getParentSessionId());
        assertTrue(dto.getIsChild());
        assertEquals("test-description", dto.getDescription());
        assertEquals(now, dto.getCreateTime());
        assertEquals(now, dto.getUpdateTime());
    }

    @Test
    void toDTO_parentSessionId为null_不报错() {
        Session entity = new Session();
        entity.setId(1L);
        entity.setIsChild(false);
        entity.setTitle("no-parent");

        when(sessionMapper.selectList(any())).thenReturn(List.of(entity));

        List<SessionDTO> result = sessionService.listChildSessions(99L);

        assertEquals(1, result.size());
        assertNull(result.get(0).getParentSessionId());
    }

    @Test
    void toDTO_description为null_不报错() {
        Session entity = new Session();
        entity.setId(1L);
        entity.setIsChild(true);
        entity.setParentSessionId(5L);
        entity.setTitle("null-desc");

        when(sessionMapper.selectList(any())).thenReturn(List.of(entity));

        List<SessionDTO> result = sessionService.listChildSessions(5L);

        assertEquals(1, result.size());
        assertNull(result.get(0).getDescription());
    }

    @Test
    void listChildSessions_parentId为null_仍执行查询() {
        sessionService.listChildSessions(null);

        verify(sessionMapper).selectList(any());
    }
}
