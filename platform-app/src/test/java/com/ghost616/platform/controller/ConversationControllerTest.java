package com.ghost616.platform.controller;

import com.ghost616.platform.dto.session.SessionMessageDTO;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.ConversationIdDTO;
import com.ghost616.platform.service.session.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    private static final Pattern CONVERSATION_ID_PATTERN = Pattern.compile("^[0-9a-z_]+$");

    @Mock
    private SessionService sessionService;

    private ConversationController controller;

    @BeforeEach
    void setUp() {
        controller = new ConversationController(sessionService);
    }

    @Test
    void generateConversationId_shouldReturnSuccess() {
        ApiResponse<ConversationIdDTO> response = controller.generateConversationId();

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getConversationId());
    }

    @Test
    void generateConversationId_shouldReturn24LengthMatchingCharset() {
        ApiResponse<ConversationIdDTO> response = controller.generateConversationId();

        String conversationId = response.getData().getConversationId();
        assertEquals(24, conversationId.length());
        assertTrue(CONVERSATION_ID_PATTERN.matcher(conversationId).matches());
    }

    @Test
    void generateConversationId_shouldReturnDistinctIds() {
        String id1 = controller.generateConversationId().getData().getConversationId();
        String id2 = controller.generateConversationId().getData().getConversationId();

        assertNotEquals(id1, id2);
    }

    @Test
    void getMessagesByConversationId_shouldReturnMessages() {
        SessionMessageDTO dto = SessionMessageDTO.builder()
                .id("1")
                .sessionId("10")
                .role("user")
                .content("hello")
                .sequenceNum(1)
                .createTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                .toolCalls(List.of())
                .rollback(false)
                .conversationId("conv-1")
                .build();
        when(sessionService.getMessagesByConversationId("conv-1")).thenReturn(List.of(dto));

        ApiResponse<List<SessionMessageDTO>> response =
                controller.getMessagesByConversationId("conv-1");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("hello", response.getData().get(0).getContent());
        assertEquals("conv-1", response.getData().get(0).getConversationId());
        verify(sessionService).getMessagesByConversationId("conv-1");
    }

    @Test
    void getMessagesByConversationId_noMessages_返回空列表() {
        when(sessionService.getMessagesByConversationId("conv-empty")).thenReturn(List.of());

        ApiResponse<List<SessionMessageDTO>> response =
                controller.getMessagesByConversationId("conv-empty");

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
    }
}