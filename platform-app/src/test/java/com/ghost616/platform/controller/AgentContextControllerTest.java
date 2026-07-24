package com.ghost616.platform.controller;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.platform.dto.AgentContextDTO;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.context.ConversationVariableRequest;
import com.ghost616.platform.dto.context.SessionVariableRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentContextControllerTest {

    @Mock
    private AgentContextManager agentContextManager;

    @InjectMocks
    private AgentContextController controller;

    @Test
    void getContext_shouldReturnAgentContextDTO() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);
        when(ctx.getSessionId()).thenReturn(1L);
        when(ctx.getAgentId()).thenReturn(100L);
        when(ctx.getSystemPrompt()).thenReturn("prompt");
        when(ctx.getModelId()).thenReturn(200L);
        when(ctx.getParentSessionId()).thenReturn(null);
        when(ctx.getRecentMessageCount()).thenReturn(10);
        when(ctx.getHistory()).thenReturn(List.of());
        when(ctx.getTools()).thenReturn(List.of());
        when(ctx.getSkills()).thenReturn(List.of());
        when(ctx.getProjectDir()).thenReturn("/project");
        when(ctx.getSessionVariableKeys()).thenReturn(Set.of());
        when(ctx.getConversationVariableKeys()).thenReturn(Set.of());

        ApiResponse<AgentContextDTO> response = controller.getContext(1L);

        assertTrue(response.isSuccess());
        AgentContextDTO dto = response.getData();
        assertNotNull(dto);
        assertEquals(1L, dto.getSessionId());
        assertEquals(100L, dto.getAgentId());
        assertEquals("prompt", dto.getSystemPrompt());
        assertEquals(200L, dto.getModelId());
        assertNull(dto.getParentSessionId());
        assertEquals(10, dto.getRecentMessageCount());
    }

    @Test
    void getContext_shouldMapHistoryEntries() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);

        AgentExecutionContext.HistoryEntry entry = new AgentExecutionContext.HistoryEntry(
                "user", "hello", null, "call-1", 1,
                LocalDateTime.of(2026, 7, 24, 12, 0), List.of(), null);
        when(ctx.getHistory()).thenReturn(List.of(entry));
        when(ctx.getSessionVariableKeys()).thenReturn(Set.of());
        when(ctx.getConversationVariableKeys()).thenReturn(Set.of());

        ApiResponse<AgentContextDTO> response = controller.getContext(1L);

        assertTrue(response.isSuccess());
        List<AgentContextDTO.HistoryEntryDTO> history = response.getData().getHistory();
        assertEquals(1, history.size());
        assertEquals("user", history.get(0).getRole());
        assertEquals("hello", history.get(0).getContent());
        assertEquals(1, history.get(0).getSequenceNum());
    }

    @Test
    void getContext_shouldMapSessionVariables() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);
        when(ctx.getHistory()).thenReturn(List.of());
        when(ctx.getSessionVariableKeys()).thenReturn(Set.of("key1", "key2"));
        when(ctx.getSessionVariable("key1")).thenReturn("value1");
        when(ctx.getSessionVariable("key2")).thenReturn("value2");
        when(ctx.getConversationVariableKeys()).thenReturn(Set.of());

        ApiResponse<AgentContextDTO> response = controller.getContext(1L);

        assertTrue(response.isSuccess());
        Map<String, String> vars = response.getData().getSessionVariables();
        assertEquals(2, vars.size());
        assertEquals("value1", vars.get("key1"));
        assertEquals("value2", vars.get("key2"));
    }

    @Test
    void getContext_shouldMapConversationVariables() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);
        when(ctx.getHistory()).thenReturn(List.of());
        when(ctx.getSessionVariableKeys()).thenReturn(Set.of());
        when(ctx.getConversationVariableKeys()).thenReturn(Set.of("ck1", "ck2"));
        when(ctx.getConversationVariable("ck1")).thenReturn("cv1");
        when(ctx.getConversationVariable("ck2")).thenReturn("cv2");

        ApiResponse<AgentContextDTO> response = controller.getContext(1L);

        assertTrue(response.isSuccess());
        Map<String, String> vars = response.getData().getConversationVariables();
        assertEquals(2, vars.size());
        assertEquals("cv1", vars.get("ck1"));
        assertEquals("cv2", vars.get("ck2"));
    }

    @Test
    void getContext_shouldReturnErrorWhenSessionContextNotFound() {
        when(agentContextManager.get(999L)).thenReturn(null);

        ApiResponse<AgentContextDTO> response = controller.getContext(999L);

        assertFalse(response.isSuccess());
        assertEquals("CONTEXT-001", response.getCode());
    }

    @Test
    void putSessionVariable_shouldPutVariable() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);

        SessionVariableRequest body = new SessionVariableRequest("varKey", "varValue");
        ApiResponse<Void> response = controller.putSessionVariable(1L, body);

        assertTrue(response.isSuccess());
        verify(ctx).putSessionVariable("varKey", "varValue");
    }

    @Test
    void putSessionVariable_shouldReturnErrorWhenSessionContextNotFound() {
        when(agentContextManager.get(999L)).thenReturn(null);

        ApiResponse<Void> response = controller.putSessionVariable(999L, new SessionVariableRequest("k", "v"));

        assertFalse(response.isSuccess());
        assertEquals("CONTEXT-001", response.getCode());
    }

    @Test
    void putConversationVariable_shouldPutVariable() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);

        ConversationVariableRequest body = new ConversationVariableRequest("convKey", "convValue");
        ApiResponse<Void> response = controller.putConversationVariable(1L, body);

        assertTrue(response.isSuccess());
        verify(ctx).putConversationVariable("convKey", "convValue");
    }

    @Test
    void putConversationVariable_shouldReturnErrorWhenSessionContextNotFound() {
        when(agentContextManager.get(999L)).thenReturn(null);

        ApiResponse<Void> response = controller.putConversationVariable(999L, new ConversationVariableRequest("k", "v"));

        assertFalse(response.isSuccess());
        assertEquals("CONTEXT-001", response.getCode());
    }

    @Test
    void putSessionVariable_shouldHandleMissingKey() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);

        SessionVariableRequest body = SessionVariableRequest.builder().value("val").build();
        ApiResponse<Void> response = controller.putSessionVariable(1L, body);

        assertTrue(response.isSuccess());
        verify(ctx).putSessionVariable(null, "val");
    }

    @Test
    void putSessionVariable_shouldHandleMissingValue() {
        AgentContextManager.AgentSessionContext sessionCtx = mock(AgentContextManager.AgentSessionContext.class);
        AgentExecutionContext ctx = mock(AgentExecutionContext.class);
        when(agentContextManager.get(1L)).thenReturn(sessionCtx);
        when(sessionCtx.context()).thenReturn(ctx);

        SessionVariableRequest body = SessionVariableRequest.builder().key("k").build();
        ApiResponse<Void> response = controller.putSessionVariable(1L, body);

        assertTrue(response.isSuccess());
        verify(ctx).putSessionVariable("k", null);
    }
}
