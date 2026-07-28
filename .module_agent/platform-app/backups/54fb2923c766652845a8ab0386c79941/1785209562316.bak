package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.session.CreateSessionRequest;
import com.ghost616.platform.dto.session.SessionDTO;
import com.ghost616.platform.dto.session.SubSessionDataDTO;
import com.ghost616.platform.service.session.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.platform.service.agent.DefaultSubSessionCallback;


@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final DefaultSubSessionCallback subSessionCallback;

    @GetMapping
    public ApiResponse<List<SessionDTO>> listSessions(@RequestParam(required = false) Long agentId) {
        List<SessionDTO> result = sessionService.listSessions(agentId);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<SessionDTO> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionDTO result = sessionService.createSession(
                request.getAgentId(), request.getModelId(), request.getTitle());
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<SessionDTO> getSession(@PathVariable Long id) {
        SessionDTO result = sessionService.getSession(id);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<MessageDataProvider.MessageDTO>> getMessages(@PathVariable Long id) {
        List<MessageDataProvider.MessageDTO> result = sessionService.getMessages(id);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/rollback")
    public ApiResponse<Integer> rollback(@PathVariable Long id) {
        int deleted = sessionService.rollback(id);
        return ApiResponse.success(deleted);
    }

    @GetMapping("/{id}/children")
    public ApiResponse<List<SessionDTO>> listChildSessions(@PathVariable Long id) {
        List<SessionDTO> result = sessionService.listChildSessions(id);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/sub-session-data")
    public ApiResponse<SubSessionDataDTO> getSubSessionData(@PathVariable Long id) {
        DefaultSubSessionCallback.SubSessionData data = subSessionCallback.getSubSessionData(id);
        if (data == null) {
            return ApiResponse.success(null);
        }
        SubSessionDataDTO result = SubSessionDataDTO.builder()
                .childSessionId(data.getChildSessionId())
                .userMessage(data.getUserMessage())
                .thinking(data.getThinking())
                .build();
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/complete-sub-session")
    public ApiResponse<Void> completeSubSession(@PathVariable Long id) {
        DefaultSubSessionCallback.SubSessionData data = subSessionCallback.getSubSessionData(id);
        if (data == null) {
            return ApiResponse.fail(ErrorCode.SUB_SESSION_DATA_NOT_FOUND);
        }
        List<MessageDataProvider.MessageDTO> messages = sessionService.getMessages(data.getChildSessionId());
        if (messages == null || messages.isEmpty()) {
            return ApiResponse.fail(ErrorCode.CHILD_SESSION_NO_MESSAGES);
        }
        MessageDataProvider.MessageDTO lastAssistantMsg = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("assistant".equals(messages.get(i).role())) {
                lastAssistantMsg = messages.get(i);
                break;
            }
        }
        if (lastAssistantMsg == null) {
            return ApiResponse.fail(ErrorCode.CHILD_SESSION_NO_MESSAGES);
        }
        List<ToolCall> toolCalls = null;
        if (lastAssistantMsg.toolCalls() != null && !lastAssistantMsg.toolCalls().isEmpty()) {
            toolCalls = lastAssistantMsg.toolCalls().stream()
                    .map(tc -> ToolCall.builder()
                            .id(tc.toolCallId())
                            .name(tc.toolCallName())
                            .arguments(tc.toolCallArguments())
                            .build())
                    .toList();
        }
        Message message = Message.builder()
                .role(lastAssistantMsg.role())
                .content(lastAssistantMsg.content())
                .reasoning(lastAssistantMsg.reasoning())
                .toolCallId(lastAssistantMsg.toolCallId())
                .toolCalls(toolCalls)
                .build();
        data.getMessageResult().complete(message);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ApiResponse.success(null);
    }
}
