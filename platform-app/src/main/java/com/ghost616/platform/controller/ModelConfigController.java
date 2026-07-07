package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.model.ModelConfigDTO;
import com.ghost616.platform.dto.model.ModelCreateRequest;
import com.ghost616.platform.dto.model.ModelUpdateRequest;
import com.ghost616.platform.dto.model.PlatformConfigResponse;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.enums.PlatformType;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.service.model.ModelConfigService;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatResponse;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;


@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;
    private final ModelConfigMapper modelConfigMapper;
    private final ModelInvokerManager modelInvokerManager;

    @GetMapping
    public ApiResponse<List<ModelConfigDTO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) PlatformType platformType,
            @RequestParam(required = false) CommonStatus status) {
        List<ModelConfigDTO> result = modelConfigService.list(name, platformType, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<ModelConfigDTO> getById(@PathVariable Long id) {
        ModelConfigDTO result = modelConfigService.getById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<ModelConfigDTO> create(@Valid @RequestBody ModelCreateRequest request) {
        ModelConfigDTO result = modelConfigService.create(request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelConfigDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody ModelUpdateRequest request) {
        ModelConfigDTO result = modelConfigService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelConfigService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ModelConfigDTO> toggleStatus(@PathVariable Long id,
                                                    @RequestParam CommonStatus status) {
        ModelConfigDTO result = modelConfigService.toggleStatus(id, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/platform-config")
    public ApiResponse<List<PlatformConfigResponse>> platformConfig() {
        List<PlatformConfigResponse> configs = Arrays.stream(PlatformType.values())
                .map(pt -> PlatformConfigResponse.builder()
                        .platformType(pt.name())
                        .defaultBaseUrl(pt.getDefaultBaseUrl())
                        .modelNames(pt.getDefaultModelNames())
                        .build())
                .toList();
        return ApiResponse.success(configs);
    }

    @PostMapping("/{id}/verify")
    public ApiResponse<Boolean> verify(@PathVariable Long id) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        ModelConfigData configData = new ModelConfigData(config.getId(), config.getApiKey(), config.getBaseUrl(), config.getModelName(), config.getTemperature(), config.getMaxTokens(), config.getPlatformType().name());
        ModelInvoker invoker = modelInvokerManager.getInvoker(configData);
        boolean result = invoker.verify();
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/chat")
    public ApiResponse<ChatResponse> chat(@PathVariable Long id,
                                          @RequestBody ChatRequest request) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        ModelConfigData configData = new ModelConfigData(config.getId(), config.getApiKey(), config.getBaseUrl(), config.getModelName(), config.getTemperature(), config.getMaxTokens(), config.getPlatformType().name());
        ModelInvoker invoker = modelInvokerManager.getInvoker(configData);
        ChatResponse response = invoker.invoke(request);
        return ApiResponse.success(response);
    }

    @PostMapping(path = "/{id}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatChunk>> chatStream(@PathVariable Long id,
                                                        @RequestBody ChatRequest request) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            return Flux.error(new BusinessException(ErrorCode.MODEL_NOT_FOUND));
        }
        ModelConfigData configData = new ModelConfigData(config.getId(), config.getApiKey(), config.getBaseUrl(), config.getModelName(), config.getTemperature(), config.getMaxTokens(), config.getPlatformType().name());
        ModelInvoker invoker = modelInvokerManager.getInvoker(configData);
        return invoker.invokeStream(request)
                .map(chunk -> ServerSentEvent.<ChatChunk>builder().data(chunk).build());
    }

    @GetMapping("/{id}/invoker")
    public ApiResponse<Map<String, String>> getInvokerInfo(@PathVariable Long id) {
        ModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        ModelConfigData configData = new ModelConfigData(config.getId(), config.getApiKey(), config.getBaseUrl(), config.getModelName(), config.getTemperature(), config.getMaxTokens(), config.getPlatformType().name());
        ModelInvoker invoker = modelInvokerManager.getInvoker(configData);
        Map<String, String> info = new HashMap<>();
        info.put("platformType", config.getPlatformType().name());
        info.put("invokerClass", invoker.getClass().getSimpleName());
        return ApiResponse.success(info);
    }
}
