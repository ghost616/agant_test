package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.session.CreateSessionRequest;
import com.ghost616.platform.dto.session.SessionDTO;
import com.ghost616.platform.service.agent.SessionManager;
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

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

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
    public ApiResponse<List<SessionManager.MessageDTO>> getMessages(@PathVariable Long id) {
        List<SessionManager.MessageDTO> result = sessionService.getMessages(id);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ApiResponse.success(null);
    }
}
