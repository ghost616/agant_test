package com.ghost616.platform.controller;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.ChatDataCacheManager;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import com.ghost616.platform.service.agent.DefaultChatDataCacheProvider;
import com.ghost616.platform.service.evaluation.EvaluationExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationExecutionController {

    private final EvaluationExecutionService evaluationExecutionService;
    private final ChatDataCacheManager chatDataCacheManager;
    private final DefaultChatDataCacheProvider defaultChatDataCacheProvider;

    @PostMapping("/{id}/execute")
    public ApiResponse<EvaluationExecutionStatusDTO> execute(@PathVariable Long id) {
        EvaluationExecutionStatusDTO result = evaluationExecutionService.execute(id);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/execute/status")
    public ApiResponse<EvaluationExecutionStatusDTO> getStatus(@PathVariable Long id) {
        EvaluationExecutionStatusDTO result = evaluationExecutionService.getStatus(id);
        return ApiResponse.success(result);
    }

    @GetMapping(value = "/session/{executionSessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatChunk>> stream(@PathVariable Long executionSessionId) {
        String sessionId = String.valueOf(executionSessionId);
        List<String> cacheIds = defaultChatDataCacheProvider.getCacheIdsBySessionId(sessionId);
        if (cacheIds.isEmpty()) {
            return Flux.empty();
        }
        return chatDataCacheManager.getStream(cacheIds.get(0), 0);
    }
}
