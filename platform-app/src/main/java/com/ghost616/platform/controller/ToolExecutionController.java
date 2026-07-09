package com.ghost616.platform.controller;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.platform.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ToolExecutionController {

    private final ToolExecutionService toolExecutionService;

    @PostMapping("/{sessionId}/execute-tools")
    public ApiResponse<ToolExecutionService.ToolExecutionResult> executeTools(@PathVariable Long sessionId) {
        ToolExecutionService.ToolExecutionResult result = toolExecutionService.executeTool(sessionId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{sessionId}/tool-status")
    public ApiResponse<ToolExecutionService.ToolStatusResult> toolStatus(@PathVariable Long sessionId,
                                                                          @RequestParam String toolId) {
        ToolExecutionService.ToolStatusResult result = toolExecutionService.getToolStatus(sessionId, toolId);
        return ApiResponse.success(result);
    }

    @PostMapping(value = "/{sessionId}/continue", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatChunk>> continueChat(@PathVariable Long sessionId) {
        return toolExecutionService.continueAfterTools(sessionId);
    }
}
