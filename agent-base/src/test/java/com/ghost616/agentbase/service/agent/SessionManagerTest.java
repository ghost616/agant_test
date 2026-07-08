package com.ghost616.agentbase.service.agent;

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

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager(dataProvider);
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
                .sessionId(1L)
                .content("hello");
        BusinessException ex = assertThrows(BusinessException.class, builder::save);
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("role 不能为空"));
    }

    @Test
    void save_content为null时抛出BusinessException() {
        SessionManager.MessageSaveBuilder builder = sessionManager.messageSave()
                .sessionId(1L)
                .role("user");
        BusinessException ex = assertThrows(BusinessException.class, builder::save);
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("content 不能为空"));
    }

    @Test
    void save_参数均非null时正常调用dataProvider() {
        when(dataProvider.saveMessage(1L, "user", "hello", null, null, null, null))
                .thenReturn(100L);

        Long result = sessionManager.messageSave()
                .sessionId(1L)
                .role("user")
                .content("hello")
                .save();

        assertEquals(100L, result);
        verify(dataProvider).saveMessage(1L, "user", "hello", null, null, null, null);
    }

    @Test
    void save_参数均非null时正常调用dataProvider_withAllFields() {
        var toolCalls = java.util.List.of(
                new MessageDataProvider.ToolCallData("tc1", "getWeather", "{}"));
        when(dataProvider.saveMessage(1L, "assistant", "response", "thinking...",
                "tc1", "result_ok", toolCalls)).thenReturn(200L);

        Long result = sessionManager.messageSave()
                .sessionId(1L)
                .role("assistant")
                .content("response")
                .reasoning("thinking...")
                .toolCallId("tc1")
                .toolResult("result_ok")
                .toolCalls(toolCalls)
                .save();

        assertEquals(200L, result);
        verify(dataProvider).saveMessage(1L, "assistant", "response", "thinking...",
                "tc1", "result_ok", toolCalls);
    }
}
