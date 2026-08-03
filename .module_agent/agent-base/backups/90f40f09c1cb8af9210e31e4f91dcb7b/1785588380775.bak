package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionManagerTest {

    @Mock
    private MessageDataProvider dataProvider;

    private AgentComponentRegistry registry;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        registry = new AgentComponentRegistry();
        registry.setMessageDataProvider(dataProvider);
        sessionManager = new SessionManager(registry);
    }

    @Test
    void save_sessionId为null时抛出BusinessException() {
        SessionManager.MessageSaveBuilder builder = sessionManager.messageSave()
                .role("user")
                .content("hello");
        BusinessException ex = assertThrows(BusinessException.class, builder::save);
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("sessionId 不能为空"));
    }

    @Test
    void save_role为null时抛出BusinessException() {
        SessionManager.MessageSaveBuilder builder = sessionManager.messageSave()
                .sessionId("1")
                .content("hello");
        BusinessException ex = assertThrows(BusinessException.class, builder::save);
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("role 不能为空"));
    }

    @Test
    void save_content为null时抛出BusinessException() {
        SessionManager.MessageSaveBuilder builder = sessionManager.messageSave()
                .sessionId("1")
                .role("user");
        BusinessException ex = assertThrows(BusinessException.class, builder::save);
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("content 不能为空"));
    }

    @Test
    void save_参数均非null时正常调用dataProvider() {
        when(dataProvider.saveMessage("1", "user", "hello", null, null, null, null, null, null, null))
                .thenReturn("100");

        String result = sessionManager.messageSave()
                .sessionId("1")
                .role("user")
                .content("hello")
                .save();

        assertEquals("100", result);
        verify(dataProvider).saveMessage("1", "user", "hello", null, null, null, null, null, null, null);
    }

    @Test
    void save_参数均非null时正常调用dataProvider_withAllFields() {
        var toolCalls = java.util.List.of(
                new MessageDataProvider.ToolCallData("tc1", "getWeather", "{}"));
        when(dataProvider.saveMessage("1", "assistant", "response", "thinking...",
                "tc1", "result_ok", toolCalls, null, null, null)).thenReturn("200");

        String result = sessionManager.messageSave()
                .sessionId("1")
                .role("assistant")
                .content("response")
                .reasoning("thinking...")
                .toolCallId("tc1")
                .toolResult("result_ok")
                .toolCalls(toolCalls)
                .save();

        assertEquals("200", result);
        verify(dataProvider).saveMessage("1", "assistant", "response", "thinking...",
                "tc1", "result_ok", toolCalls, null, null, null);
    }
}
