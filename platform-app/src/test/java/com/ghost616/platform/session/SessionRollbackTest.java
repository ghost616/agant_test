package com.ghost616.platform.session;

import com.ghost616.platform.controller.SessionController;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.session.SessionService;
import com.ghost616.platform.service.session.SessionServiceImpl;
import com.ghost616.platform.repository.AgentToolMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;


@ExtendWith(MockitoExtension.class)
class SessionRollbackTest {

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
    @Mock
    private com.ghost616.platform.repository.MessageMapper messageMapper;
    @Mock
    private com.ghost616.platform.service.agent.DefaultMessageDataProvider defaultMessageDataProvider;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private final Long sessionId = 1L;

    @Test
    void rollback_正常回退_返回删除数量() {
        Session session = new Session();
        session.setId(sessionId);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(sessionManager.rollbackToLastUserMessage("1")).thenReturn(3);

        int deleted = sessionService.rollback(sessionId);

        assertEquals(3, deleted);
        verify(sessionManager).rollbackToLastUserMessage("1");
        verify(agentContextManager).remove("1");
    }

    @Test
    void rollback_会话不存在_抛出SESSION_NOT_FOUND() {
        when(sessionMapper.selectById(sessionId)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.rollback(sessionId));
        assertEquals(ErrorCode.SESSION_NOT_FOUND, ex.getErrorCode());
        verify(sessionManager, never()).rollbackToLastUserMessage(any());
        verify(agentContextManager, never()).remove(any());
    }

    @Test
    void rollback_会话无用户消息_抛出SESSION_NO_USER_MESSAGE() {
        Session session = new Session();
        session.setId(sessionId);
        when(sessionMapper.selectById(sessionId)).thenReturn(session);
        when(sessionManager.rollbackToLastUserMessage("1"))
                .thenThrow(new BusinessException(ErrorCode.SESSION_NO_USER_MESSAGE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sessionService.rollback(sessionId));
        assertEquals(ErrorCode.SESSION_NO_USER_MESSAGE, ex.getErrorCode());
        verify(agentContextManager, never()).remove(any());
    }

    @Test
    void rollback_SessionManager有用户消息_删除后续消息() {
        String sid = "99";
        AgentComponentRegistry registry = mock(AgentComponentRegistry.class);
        MessageDataProvider messageDataProvider = mock(MessageDataProvider.class);
        when(registry.getMessageDataProvider()).thenReturn(messageDataProvider);
        SessionManager realManager = new SessionManager(registry);

        when(messageDataProvider.rollbackToLastUserMessage(sid)).thenReturn(2);

        int deleted = realManager.rollbackToLastUserMessage(sid);

        assertEquals(2, deleted);
    }

    @Test
    void rollback_SessionManager无用户消息_抛出异常() {
        String sid = "99";
        AgentComponentRegistry registry = mock(AgentComponentRegistry.class);
        MessageDataProvider messageDataProvider = mock(MessageDataProvider.class);
        when(registry.getMessageDataProvider()).thenReturn(messageDataProvider);
        SessionManager realManager = new SessionManager(registry);

        when(messageDataProvider.rollbackToLastUserMessage(sid))
                .thenThrow(new BusinessException(ErrorCode.SESSION_NO_USER_MESSAGE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> realManager.rollbackToLastUserMessage(sid));
        assertEquals(ErrorCode.SESSION_NO_USER_MESSAGE, ex.getErrorCode());
    }
}
