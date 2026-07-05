package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.service.agent.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.platform.dto.chat.ChatRequest;
import com.ghost616.agentbase.service.agent.AgentContextManager;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AgentContextManager agentContextManager;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatChunk>> chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @PostMapping("/chat/{sessionId}/stop")
    public ApiResponse<Map<String, Object>> stopChat(@PathVariable Long sessionId) {
        AgentContextManager.AgentSessionContext sessionCtx = agentContextManager.get(sessionId);
        if (sessionCtx != null) {
            sessionCtx.mutator().setStopped();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", "stopped");
        return ApiResponse.success(result);
    }
}
