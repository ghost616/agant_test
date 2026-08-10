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
    void stream_存在缓存_返回缓存流() {
        when(defaultChatDataCacheProvider.getCacheIdsBySessionId("200"))
                .thenReturn(List.of("cache-1"));
        ChatChunk chunk = ChatChunk.builder().delta("test").build();
        Flux<ServerSentEvent<ChatChunk>> expectedFlux = Flux.just(
                ServerSentEvent.builder(chunk).build());
        when(chatDataCacheManager.getStream("cache-1", 0)).thenReturn(expectedFlux);

        Flux<ServerSentEvent<ChatChunk>> result = controller.stream(200L);

        assertNotNull(result);
        verify(defaultChatDataCacheProvider).getCacheIdsBySessionId("200");
        verify(chatDataCacheManager).getStream("cache-1", 0);
    }

    @Test
    void stream_无缓存_返回空Flux且不调用getStream() {
        when(defaultChatDataCacheProvider.getCacheIdsBySessionId("200"))
                .thenReturn(List.of());

        Flux<ServerSentEvent<ChatChunk>> result = controller.stream(200L);

        assertNotNull(result);
        verify(defaultChatDataCacheProvider).getCacheIdsBySessionId("200");
        verify(chatDataCacheManager, never()).getStream(anyString(), anyInt());
    }

    @Test
    void stream_有多个缓存_取第一个缓存ID() {
        when(defaultChatDataCacheProvider.getCacheIdsBySessionId("200"))
                .thenReturn(List.of("cache-1", "cache-2"));
        when(chatDataCacheManager.getStream("cache-1", 0))
                .thenReturn(Flux.empty());

        Flux<ServerSentEvent<ChatChunk>> result = controller.stream(200L);

        assertNotNull(result);
        verify(defaultChatDataCacheProvider).getCacheIdsBySessionId("200");
        verify(chatDataCacheManager).getStream("cache-1", 0);
        verify(chatDataCacheManager, never()).getStream("cache-2", 0);
    }

    @Test
    void cacheStatus_存在缓存_hasCache为true() {
        when(defaultChatDataCacheProvider.getCacheIdsBySessionId("200"))
                .thenReturn(List.of("cache-1"));

        ApiResponse<Map<String, Boolean>> response = controller.cacheStatus(200L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(Boolean.TRUE, response.getData().get("hasCache"));
        verify(defaultChatDataCacheProvider).getCacheIdsBySessionId("200");
    }

    @Test
    void cacheStatus_无缓存_hasCache为false() {
        when(defaultChatDataCacheProvider.getCacheIdsBySessionId("200"))
                .thenReturn(List.of());

        ApiResponse<Map<String, Boolean>> response = controller.cacheStatus(200L);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(Boolean.FALSE, response.getData().get("hasCache"));
        verify(defaultChatDataCacheProvider).getCacheIdsBySessionId("200");
    }
}
