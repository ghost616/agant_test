package com.ghost616.platform.service.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.dto.tool.ToolConfigDTO;
import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;
import com.ghost616.platform.entity.ToolConfig;
import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.enums.ToolType;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.event.ToolChangedEvent;
import com.ghost616.platform.repository.ToolConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.ghost616.platform.service.agent.invoker.McpAuthConfigParser;
import com.ghost616.platform.service.agent.invoker.McpJsonRpcClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolConfigServiceImpl implements ToolConfigService {

    private final ToolConfigMapper toolConfigMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<ToolConfigDTO> list(String name, ToolType toolType, CommonStatus status) {
        LambdaQueryWrapper<ToolConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ToolConfig::getName, name);
        }
        if (toolType != null) {
            wrapper.eq(ToolConfig::getToolType, toolType);
        }
        if (status != null) {
            wrapper.eq(ToolConfig::getStatus, status);
        }
        wrapper.orderByDesc(ToolConfig::getCreateTime);

        List<ToolConfig> entities = toolConfigMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public ToolConfigDTO getById(Long id) {
        ToolConfig entity = toolConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    public ToolConfigDTO create(ToolCreateRequest request) {
        checkNameDuplicate(request.getName(), null);

        ToolConfig entity = new ToolConfig();
        entity.setName(request.getName());
        entity.setToolType(request.getToolType());
        entity.setDescription(request.getDescription());
        entity.setParameterSchema(normalizeParameterSchema(request.getParameterSchema()));
        entity.setReturnSchema(request.getReturnSchema());
        validateImplPath(request.getImplPath(), request.getToolType(), request.getAuthConfig());
        entity.setImplPath(request.getImplPath());
        entity.setAuthConfig(request.getAuthConfig());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : CommonStatus.ENABLED);

        toolConfigMapper.insert(entity);
        return toDTO(entity);
    }

    @Override
    public ToolConfigDTO update(Long id, ToolUpdateRequest request) {
        ToolConfig entity = toolConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }

        if (StringUtils.isNotBlank(request.getName())) {
            checkNameDuplicate(request.getName(), id);
            entity.setName(request.getName());
        }
        if (request.getToolType() != null) {
            entity.setToolType(request.getToolType());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getParameterSchema() != null) {
            entity.setParameterSchema(normalizeParameterSchema(request.getParameterSchema()));
        }
        if (request.getReturnSchema() != null) {
            entity.setReturnSchema(request.getReturnSchema());
        }
        if (request.getImplPath() != null) {
            validateImplPath(request.getImplPath(), entity.getToolType(), request.getAuthConfig());
            entity.setImplPath(request.getImplPath());
        }
        if (request.getAuthConfig() != null) {
            entity.setAuthConfig(request.getAuthConfig());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        toolConfigMapper.updateById(entity);
        eventPublisher.publishEvent(new ToolChangedEvent(this, id));
        return toDTO(entity);
    }

    @Override
    public void delete(Long id) {
        ToolConfig entity = toolConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        toolConfigMapper.deleteById(id);
        eventPublisher.publishEvent(new ToolChangedEvent(this, id));
    }

    @Override
    public ToolConfigDTO toggleStatus(Long id, CommonStatus status) {
        ToolConfig entity = toolConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        entity.setStatus(status);
        toolConfigMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public ToolConfigDTO getImplByName(String name) {
        LambdaQueryWrapper<ToolConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToolConfig::getName, name);
        ToolConfig entity = toolConfigMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        return toDTO(entity);
    }

    private String normalizeParameterSchema(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            return raw;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                    "参数 Schema 不是有效的 JSON: " + e.getMessage());
        }
    }

    private void validateImplPath(String implPath, ToolType toolType, String authConfig) {
        if (toolType == ToolType.JAVA) {
            try {
                Class.forName(implPath);
            } catch (ClassNotFoundException e) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                        "Java 实现类不存在: " + implPath);
            }
        } else if (toolType == ToolType.TYPESCRIPT) {
            if (!Files.isDirectory(Path.of(implPath))) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                        "TypeScript 实现目录不存在: " + implPath);
            }
            if (!Files.exists(Path.of(implPath, "index.ts"))) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                        "TypeScript 实现目录下缺少 index.ts: " + implPath);
            }
        } else if (toolType == ToolType.PYTHON) {
            if (!Files.isDirectory(Path.of(implPath))) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                        "Python 实现目录不存在: " + implPath);
            }
            if (!Files.exists(Path.of(implPath, "index.py"))) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                        "Python 实现目录下缺少 index.py: " + implPath);
            }
        } else if (toolType == ToolType.MCP_HTTP) {
            if (!implPath.startsWith("http://") && !implPath.startsWith("https://")) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                        "MCP HTTP URL 必须以 http:// 或 https:// 开头: " + implPath);
            }
            try {
                Map<String, String> authHeaders = Map.of();
                if (authConfig != null && !authConfig.isBlank()) {
                    try {
                        authHeaders = McpAuthConfigParser.parse(authConfig);
                    } catch (Exception e) {
                        log.warn("解析 MCP 认证配置失败，使用无认证连接: {} - {}", implPath, e.getMessage());
                    }
                }
                McpJsonRpcClient client = new McpJsonRpcClient(implPath, authHeaders);
                client.initialize();
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                        "MCP 服务连接失败: " + implPath + " - " + e.getMessage());
            }
        }
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<ToolConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToolConfig::getName, name);
        if (excludeId != null) {
            wrapper.ne(ToolConfig::getId, excludeId);
        }
        if (toolConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.TOOL_ALREADY_EXISTS);
        }
    }

    private ToolConfigDTO toDTO(ToolConfig entity) {
        return ToolConfigDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .toolType(entity.getToolType())
                .description(entity.getDescription())
                .parameterSchema(entity.getParameterSchema())
                .returnSchema(entity.getReturnSchema())
                .implPath(entity.getImplPath())
                .authConfig(entity.getAuthConfig())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
