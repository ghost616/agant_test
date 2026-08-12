package com.ghost616.platform.controller;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.service.agent.ChatDataCacheManager;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import com.ghost616.platform.service.agent.DefaultChatDataCacheProvider;
import com.ghost616.platform.service.evaluation.EvaluationExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationExecutionControllerTest {

    @Mock
    private EvaluationExecutionService evaluationExecutionService;

    @Mock
    private ChatDataCacheManager chatDataCacheManager;

    @Mock
    private DefaultChatDataCacheProvider defaultChatDataCacheProvider;

    @InjectMocks
    private EvaluationExecutionController controller;

    @Test
    void execute_调用服务并返回结果() {
        EvaluationExecutionStatusDTO dto = EvaluationExecutionStatusDTO.builder()
                .evaluationId(1L)
                .executionSessionId(200L)
                .status("COMPLETED")
                .currentStep(1)
                .totalSteps(1)
                .build();
        when(evaluationExecutionService.execute(1L)).thenReturn(dto);

        ApiResponse<EvaluationExecutionStatusDTO> response = controller.execute(1L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(dto, response.getData());
        verify(evaluationExecutionService).execute(1L);
    }

    @Test
    void getStatus_调用服务并返回结果() {
        EvaluationExecutionStatusDTO dto = EvaluationExecutionStatusDTO.builder()
                .evaluationId(1L)
                .executionSessionId(200L)
                .status("RUNNING")
                .currentStep(1)
                .totalSteps(3)
                .build();
        when(evaluationExecutionService.getStatus(1L)).thenReturn(dto);

        ApiResponse<EvaluationExecutionStatusDTO> response = controller.getStatus(1L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(dto, response.getData());
        verify(evaluationExecutionService).getStatus(1L);
    }

    @Test
    void stream_直接调用getStream并返回缓存流() {
        ChatChunk chunk = ChatChunk.builder().delta("test").build();
        Flux<ServerSentEvent<ChatChunk>> expectedFlux = Flux.just(
                ServerSentEvent.builder(chunk).build());
        when(chatDataCacheManager.getStream("cache-1", 0)).thenReturn(expectedFlux);

        Flux<ServerSentEvent<ChatChunk>> result = controller.stream("cache-1");

        assertNotNull(result);
        verify(chatDataCacheManager).getStream("cache-1", 0);
    }

    @Test
    void stream_不再查询getCacheIdsBySessionId() {
        ChatChunk chunk = ChatChunk.builder().delta("test").build();
        Flux<ServerSentEvent<ChatChunk>> expectedFlux = Flux.just(
                ServerSentEvent.builder(chunk).build());
        when(chatDataCacheManager.getStream("cache-1", 0)).thenReturn(expectedFlux);

        Flux<ServerSentEvent<ChatChunk>> result = controller.stream("cache-1");

        assertNotNull(result);
        verify(defaultChatDataCacheProvider, never()).getCacheIdsBySessionId(anyString());
        verify(chatDataCacheManager).getStream("cache-1", 0);
    }

    @Test
    void cacheStatus_存在缓存_hasCache为true且返回第一个缓存ID() {
        when(defaultChatDataCacheProvider.getCacheIdsBySessionId("200"))
                .thenReturn(List.of("cache-1", "cache-2"));

        ApiResponse<Map<String, Object>> response = controller.cacheStatus("200");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(Boolean.TRUE, response.getData().get("hasCache"));
        assertEquals("cache-1", response.getData().get("cacheId"));
        verify(defaultChatDataCacheProvider).getCacheIdsBySessionId("200");
    }

    @Test
    void cacheStatus_无缓存_hasCache为false且cacheId为null() {
        when(defaultChatDataCacheProvider.getCacheIdsBySessionId("200"))
                .thenReturn(List.of());

        ApiResponse<Map<String, Object>> response = controller.cacheStatus("200");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(Boolean.FALSE, response.getData().get("hasCache"));
        assertNull(response.getData().get("cacheId"));
        verify(defaultChatDataCacheProvider).getCacheIdsBySessionId("200");
    }

    @Test
    void removeCache_调用provider删除缓存() {
        ApiResponse<Void> response = controller.removeCache("cache-1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(defaultChatDataCacheProvider).removeCache("cache-1");
    }
}
