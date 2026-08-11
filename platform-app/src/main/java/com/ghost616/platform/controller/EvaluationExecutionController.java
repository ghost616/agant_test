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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/cache/status")
    public ApiResponse<Map<String, Object>> cacheStatus(@RequestParam String sessionId) {
        List<String> cacheIds = defaultChatDataCacheProvider.getCacheIdsBySessionId(sessionId);
        boolean hasCache = !cacheIds.isEmpty();
        if (hasCache) {
            String cacheId = cacheIds.get(0);
            while (true) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (defaultChatDataCacheProvider.getMaxChunkIndex(cacheId) > 0) {
                    break;
                }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("hasCache", hasCache);
        data.put("cacheId", hasCache ? cacheIds.get(0) : null);
        return ApiResponse.success(data);
    }

    @GetMapping(value = "/cache/{cacheId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatChunk>> stream(@PathVariable String cacheId) {
        return chatDataCacheManager.getStream(cacheId, 0);
    }

    @DeleteMapping("/cache/{cacheId}")
    public ApiResponse<Void> removeCache(@PathVariable String cacheId) {
        defaultChatDataCacheProvider.removeCache(cacheId);
        return ApiResponse.success(null);
    }
}
