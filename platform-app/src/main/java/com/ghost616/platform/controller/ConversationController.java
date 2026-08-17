package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.ConversationIdDTO;
import com.ghost616.platform.service.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.List;

import com.ghost616.platform.dto.session.SessionMessageDTO;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversationController {

    private static final char[] CONVERSATION_ID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz_".toCharArray();
    private static final int CONVERSATION_ID_LENGTH = 24;

    private final SessionService sessionService;

    private final SecureRandom random = new SecureRandom();

    @GetMapping("/conversation-id")
    public ApiResponse<ConversationIdDTO> generateConversationId() {
        return ApiResponse.success(new ConversationIdDTO(generateRandomConversationId()));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<SessionMessageDTO>> getMessagesByConversationId(@PathVariable String conversationId) {
        return ApiResponse.success(sessionService.getMessagesByConversationId(conversationId));
    }

    private String generateRandomConversationId() {
        StringBuilder sb = new StringBuilder(CONVERSATION_ID_LENGTH);
        for (int i = 0; i < CONVERSATION_ID_LENGTH; i++) {
            sb.append(CONVERSATION_ID_CHARS[random.nextInt(CONVERSATION_ID_CHARS.length)]);
        }
        return sb.toString();
    }
}