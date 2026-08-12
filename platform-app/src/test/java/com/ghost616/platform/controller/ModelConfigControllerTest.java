package com.ghost616.platform.controller;

import com.ghost616.agentbase.dto.model.EmbeddingRequest;
import com.ghost616.agentbase.dto.model.EmbeddingResponse;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.agentbase.enums.ModelType;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.model.ModelConfigDTO;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.service.model.ModelConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigControllerTest {

    @Mock
    private ModelConfigService modelConfigService;

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @Mock
    private ModelInvokerManager modelInvokerManager;

    @InjectMocks
    private ModelConfigController controller;

    private ModelConfig config;

    @BeforeEach
    void setUp() {
        config = new ModelConfig();
        config.setId(100L);
        config.setApiKey("sk-test");
        config.setBaseUrl("https://api.test.com");
        config.setModelName("embedding-model");
        config.setTemperature(0.7);
        config.setMaxTokens(2048);
        config.setPlatformType(PlatformType.OPENAI);
        config.setRequestType("chat");
    }

    private EmbeddingRequest request(String input) {
        return EmbeddingRequest.builder().input(input).build();
    }

    @Test
    void list_modelType参数透传给Service() {
        ModelConfigDTO dto = ModelConfigDTO.builder()
                .id(1L)
                .name("m")
                .modelType(ModelType.EMBEDDINGS)
                .build();
        when(modelConfigService.list("gpt", PlatformType.OPENAI, CommonStatus.ENABLED, ModelType.EMBEDDINGS))
                .thenReturn(List.of(dto));

        ApiResponse<List<ModelConfigDTO>> response = controller.list("gpt", PlatformType.OPENAI, CommonStatus.ENABLED, ModelType.EMBEDDINGS);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals(ModelType.EMBEDDINGS, response.getData().get(0).getModelType());
        verify(modelConfigService).list("gpt", PlatformType.OPENAI, CommonStatus.ENABLED, ModelType.EMBEDDINGS);
    }

    @Test
    void list_modelType为空时透传null() {
        when(modelConfigService.list(null, null, null, null)).thenReturn(List.of());

        ApiResponse<List<ModelConfigDTO>> response = controller.list(null, null, null, null);

        assertTrue(response.isSuccess());
        assertTrue(response.getData().isEmpty());
        verify(modelConfigService).list(null, null, null, null);
    }

    @Test
    void embed_模型不存在_抛出MODEL_NOT_FOUND() {
        when(modelConfigMapper.selectById(100L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.embed(100L, request("hello")));

        assertEquals(ErrorCode.MODEL_NOT_FOUND, ex.getErrorCode());
        verify(modelConfigMapper).selectById(100L);
        verify(modelInvokerManager, never()).getInvoker(any());
    }

    @Test
    void embed_input超过1000字符_抛出PARAM_INVALID且信息含1000() {
        when(modelConfigMapper.selectById(100L)).thenReturn(config);
        String longInput = "a".repeat(1001);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.embed(100L, request(longInput)));

        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("1000"));
        verify(modelInvokerManager, never()).getInvoker(any());
    }

    @Test
    void embed_input正好1000字符_合法并调用embed() {
        when(modelConfigMapper.selectById(100L)).thenReturn(config);
        EmbeddingResponse expected = EmbeddingResponse.builder()
                .embeddings(List.of())
                .build();
        ModelInvoker invoker = mock(ModelInvoker.class);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(invoker);
        when(invoker.embed(any(EmbeddingRequest.class))).thenReturn(expected);
        String boundaryInput = "a".repeat(1000);

        ApiResponse<EmbeddingResponse> response = controller.embed(100L, request(boundaryInput));

        assertTrue(response.isSuccess());
        assertSame(expected, response.getData());
        ArgumentCaptor<EmbeddingRequest> reqCaptor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(invoker).embed(reqCaptor.capture());
        assertEquals(boundaryInput, reqCaptor.getValue().getInput());
    }

    @Test
    void embed_input合法_复用ModelConfigData模式并返回EmbeddingResponse() {
        when(modelConfigMapper.selectById(100L)).thenReturn(config);
        EmbeddingResponse expected = EmbeddingResponse.builder()
                .embeddings(List.of(EmbeddingResponse.EmbeddingItem.builder().index(0).embedding(List.of(0.1f, 0.2f)).build()))
                .build();
        ModelInvoker invoker = mock(ModelInvoker.class);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(invoker);
        when(invoker.embed(any(EmbeddingRequest.class))).thenReturn(expected);

        ApiResponse<EmbeddingResponse> response = controller.embed(100L, request("hello"));

        assertTrue(response.isSuccess());
        assertEquals("SYS-000", response.getCode());
        assertEquals(0.1f, response.getData().getEmbeddings().get(0).getEmbedding().get(0));

        ArgumentCaptor<ModelConfigData> configCaptor = ArgumentCaptor.forClass(ModelConfigData.class);
        verify(modelInvokerManager).getInvoker(configCaptor.capture());
        ModelConfigData configData = configCaptor.getValue();
        assertEquals("100", configData.id());
        assertEquals("sk-test", configData.apiKey());
        assertEquals("https://api.test.com", configData.baseUrl());
        assertEquals("embedding-model", configData.modelName());
        assertEquals(0.7, configData.temperature());
        assertEquals(2048, configData.maxTokens());
        assertEquals(PlatformType.OPENAI.name(), configData.platformType());
        assertEquals("chat", configData.requestType());

        ArgumentCaptor<EmbeddingRequest> reqCaptor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(invoker).embed(reqCaptor.capture());
        assertEquals("hello", reqCaptor.getValue().getInput());
    }

    @Test
    void embed_input为null_跳过长度校验并调用embed() {
        when(modelConfigMapper.selectById(100L)).thenReturn(config);
        EmbeddingResponse expected = EmbeddingResponse.builder().build();
        ModelInvoker invoker = mock(ModelInvoker.class);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(invoker);
        when(invoker.embed(any(EmbeddingRequest.class))).thenReturn(expected);

        ApiResponse<EmbeddingResponse> response = controller.embed(100L, request(null));

        assertTrue(response.isSuccess());
        assertSame(expected, response.getData());
        verify(invoker).embed(any(EmbeddingRequest.class));
    }

    @Test
    void embed_input空字符串_合法并调用embed() {
        when(modelConfigMapper.selectById(100L)).thenReturn(config);
        EmbeddingResponse expected = EmbeddingResponse.builder().build();
        ModelInvoker invoker = mock(ModelInvoker.class);
        when(modelInvokerManager.getInvoker(any(ModelConfigData.class))).thenReturn(invoker);
        when(invoker.embed(any(EmbeddingRequest.class))).thenReturn(expected);

        ApiResponse<EmbeddingResponse> response = controller.embed(100L, request(""));

        assertTrue(response.isSuccess());
        verify(invoker).embed(any(EmbeddingRequest.class));
    }
}
