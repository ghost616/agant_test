package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.service.agent.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;


@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private AgentContextManager agentContextManager;

    @InjectMocks
    private ChatController chatController;

    private final Long sessionId = 1L;

    @Test
    void stopChat_会话存在_返回status_stopped() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext.AgentContextMutator mutator = mock(AgentExecutionContext.AgentContextMutator.class);
        when(agentContextManager.get(sessionId)).thenReturn(sessionCtx);
        when(sessionCtx.mutator()).thenReturn(mutator);

        ApiResponse<Map<String, Object>> response = chatController.stopChat(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("SYS-000", response.getCode());
        assertNotNull(response.getData());
        assertEquals("stopped", response.getData().get("status"));
        verify(mutator).setStopped();
    }

    @Test
    void stopChat_会话不存在_幂等返回status_stopped() {
        when(agentContextManager.get(sessionId)).thenReturn(null);

        ApiResponse<Map<String, Object>> response = chatController.stopChat(sessionId);

        assertTrue(response.isSuccess());
        assertEquals("SYS-000", response.getCode());
        assertNotNull(response.getData());
        assertEquals("stopped", response.getData().get("status"));
        verify(agentContextManager).get(sessionId);
    }
}
