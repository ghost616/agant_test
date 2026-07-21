package com.ghost616.platform.service.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.platform.dto.model.ModelConfigDTO;
import com.ghost616.platform.dto.model.ModelCreateRequest;
import com.ghost616.platform.dto.model.ModelUpdateRequest;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;


@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelInvokerManager modelInvokerManager;

    @Override
    public List<ModelConfigDTO> list(String name, PlatformType platformType, CommonStatus status) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ModelConfig::getName, name);
        }
        if (platformType != null) {
            wrapper.eq(ModelConfig::getPlatformType, platformType);
        }
        if (status != null) {
            wrapper.eq(ModelConfig::getStatus, status);
        }
        wrapper.orderByDesc(ModelConfig::getCreateTime);

        List<ModelConfig> entities = modelConfigMapper.selectList(wrapper);
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public ModelConfigDTO getById(Long id) {
        ModelConfig entity = modelConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    public ModelConfigDTO create(ModelCreateRequest request) {
        checkNameDuplicate(request.getName(), null);

        ModelConfig entity = new ModelConfig();
        entity.setName(request.getName());
        entity.setPlatformType(request.getPlatformType());
        entity.setApiKey(request.getApiKey());
        entity.setBaseUrl(StringUtils.isNotBlank(request.getBaseUrl())
                ? request.getBaseUrl()
                : request.getPlatformType().getDefaultBaseUrl());
        entity.setModelName(request.getModelName());
        entity.setTemperature(request.getTemperature());
        entity.setMaxTokens(request.getMaxTokens());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : CommonStatus.ENABLED);
        entity.setDescription(request.getDescription());

        modelConfigMapper.insert(entity);
        return toDTO(entity);
    }

    @Override
    public ModelConfigDTO update(Long id, ModelUpdateRequest request) {
        ModelConfig entity = modelConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }

        if (StringUtils.isNotBlank(request.getName())) {
            checkNameDuplicate(request.getName(), id);
            entity.setName(request.getName());
        }
        if (request.getPlatformType() != null) {
            entity.setPlatformType(request.getPlatformType());
        }
        if (request.getApiKey() != null) {
            entity.setApiKey(request.getApiKey());
        }
        if (request.getBaseUrl() != null) {
        entity.setBaseUrl(StringUtils.isNotBlank(request.getBaseUrl())
                ? request.getBaseUrl()
                : request.getPlatformType().getDefaultBaseUrl());
        }
        if (request.getModelName() != null) {
            entity.setModelName(request.getModelName());
        }
        if (request.getTemperature() != null) {
            entity.setTemperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            entity.setMaxTokens(request.getMaxTokens());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        modelConfigMapper.updateById(entity);
        modelInvokerManager.evict(id);
        return toDTO(entity);
    }

    @Override
    public void delete(Long id) {
        ModelConfig entity = modelConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        modelConfigMapper.deleteById(id);
        modelInvokerManager.evict(id);
    }

    @Override
    public ModelConfigDTO toggleStatus(Long id, CommonStatus status) {
        ModelConfig entity = modelConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        entity.setStatus(status);
        modelConfigMapper.updateById(entity);
        modelInvokerManager.evict(id);
        return toDTO(entity);
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getName, name);
        if (excludeId != null) {
            wrapper.ne(ModelConfig::getId, excludeId);
        }
        if (modelConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.MODEL_ALREADY_EXISTS);
        }
    }

    private ModelConfigDTO toDTO(ModelConfig entity) {
        return ModelConfigDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .platformType(entity.getPlatformType())
                .apiKey(entity.getApiKey())
                .baseUrl(entity.getBaseUrl())
                .modelName(entity.getModelName())
                .temperature(entity.getTemperature())
                .maxTokens(entity.getMaxTokens())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
