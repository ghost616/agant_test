package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.session.SubSessionDataDTO;
import com.ghost616.platform.service.agent.DefaultSubSessionCallback;
import com.ghost616.platform.service.session.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private DefaultSubSessionCallback subSessionCallback;

    @InjectMocks
    private SessionController controller;

    @Test
    void getSubSessionDataShouldMapThinkingField() {
        DefaultSubSessionCallback.SubSessionData data = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(data.getChildSessionId()).thenReturn(100L);
        when(data.getUserMessage()).thenReturn("hello");
        when(data.getThinking()).thenReturn(true);
        when(subSessionCallback.getSubSessionData(1L)).thenReturn(data);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(1L);

        assertTrue(response.isSuccess());
        SubSessionDataDTO dto = response.getData();
        assertNotNull(dto);
        assertEquals(100L, dto.getChildSessionId());
        assertEquals("hello", dto.getUserMessage());
        assertTrue(dto.getThinking());
    }

    @Test
    void getSubSessionDataShouldMapThinkingNull() {
        DefaultSubSessionCallback.SubSessionData data = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(data.getChildSessionId()).thenReturn(200L);
        when(data.getUserMessage()).thenReturn("test");
        when(data.getThinking()).thenReturn(null);
        when(subSessionCallback.getSubSessionData(2L)).thenReturn(data);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(2L);

        assertTrue(response.isSuccess());
        SubSessionDataDTO dto = response.getData();
        assertNull(dto.getThinking());
    }

    @Test
    void getSubSessionDataShouldMapThinkingFalse() {
        DefaultSubSessionCallback.SubSessionData data = mock(DefaultSubSessionCallback.SubSessionData.class);
        when(data.getChildSessionId()).thenReturn(300L);
        when(data.getUserMessage()).thenReturn("no");
        when(data.getThinking()).thenReturn(false);
        when(subSessionCallback.getSubSessionData(3L)).thenReturn(data);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(3L);

        assertTrue(response.isSuccess());
        SubSessionDataDTO dto = response.getData();
        assertFalse(dto.getThinking());
    }

    @Test
    void getSubSessionDataShouldReturnNullWhenDataNotFound() {
        when(subSessionCallback.getSubSessionData(999L)).thenReturn(null);

        ApiResponse<SubSessionDataDTO> response = controller.getSubSessionData(999L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }
}
