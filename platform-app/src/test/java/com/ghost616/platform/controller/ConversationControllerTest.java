package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.ConversationIdDTO;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ConversationControllerTest {

    private static final Pattern CONVERSATION_ID_PATTERN = Pattern.compile("^[0-9a-z_]+$");

    private final ConversationController controller = new ConversationController();

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
}
