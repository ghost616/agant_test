package com.ghost616.platform.service.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;
import com.ghost616.platform.entity.ToolConfig;
import com.ghost616.platform.repository.ToolConfigMapper;
import com.ghost616.platform.session.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.event.ToolChangedEvent;
import com.ghost616.platform.util.IdConverter;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.agentbase.service.agent.invoker.McpAuthConfigParser;
import com.ghost616.agentbase.service.agent.invoker.McpJsonRpcClient;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentinteg.tool.KnowledgeBaseInfoTool;
import com.ghost616.agentinteg.tool.KnowledgeFileChunkTool;
import com.ghost616.agentinteg.tool.KnowledgeFileInfoTool;
import com.ghost616.agentinteg.tool.KnowledgeSearchTool;
import org.springframework.context.annotation.Lazy;




@Slf4j
@Service
public class ToolConfigServiceImpl implements ToolConfigService {

    private final ToolConfigMapper toolConfigMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ToolManager toolManager;

    public ToolConfigServiceImpl(ToolConfigMapper toolConfigMapper,
                                 ApplicationEventPublisher eventPublisher,
                                 @Lazy ToolManager toolManager) {
        this.toolConfigMapper = toolConfigMapper;
        this.eventPublisher = eventPublisher;
        this.toolManager = toolManager;
    }

    @Override
    public List<ToolDetailDTO> list(String name, ToolType toolType, CommonStatus status) {
        Long userId = UserContextUtil.requireUserId();
        LambdaQueryWrapper<ToolConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToolConfig::getUserId, userId);
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
    public ToolDetailDTO getById(Long id) {
        ToolConfig entity = toolConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    public ToolDetailDTO create(ToolCreateRequest request) {
        Long userId = UserContextUtil.requireUserId();
        checkNameDuplicate(request.getName(), null);

        SubToolType subToolType = request.getSubToolType();
        if (subToolType == SubToolType.BROWSER) {
            if (request.getToolType() == null) {
                request.setToolType(ToolType.CUSTOM);
            }
            if (request.getToolScript() == null || request.getToolScript().isBlank()) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID, "子工具类型为 BROWSER 时 toolScript 不能为空");
            }
            validateToolScript(request.getToolScript());
        } else {
            if (request.getImplPath() == null || request.getImplPath().isBlank()) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID, "实现路径不能为空");
            }
            validateImplPath(request.getImplPath(), request.getToolType(), request.getAuthConfig());
        }

        ToolConfig entity = new ToolConfig();
        entity.setName(request.getName());
        entity.setToolType(request.getToolType());
        entity.setDescription(request.getDescription());
        entity.setParameterSchema(normalizeParameterSchema(request.getParameterSchema()));
        entity.setReturnSchema(request.getReturnSchema());
        entity.setImplPath(request.getImplPath());
        entity.setAuthConfig(request.getAuthConfig());
        entity.setSubToolType(subToolType);
        entity.setToolScript(request.getToolScript());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : CommonStatus.ENABLED);
        entity.setUserId(userId);

        toolConfigMapper.insert(entity);
        return toDTO(entity);
    }

    @Override
    public ToolDetailDTO update(Long id, ToolUpdateRequest request) {
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

        SubToolType subToolType = request.getSubToolType();
        if (subToolType == SubToolType.BROWSER) {
            if (entity.getToolType() == null) {
                entity.setToolType(ToolType.CUSTOM);
            }
            if (request.getToolScript() != null && request.getToolScript().isBlank()) {
                throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID, "子工具类型为 BROWSER 时 toolScript 不能为空");
            }
            if (request.getToolScript() != null) {
                validateToolScript(request.getToolScript());
                entity.setToolScript(request.getToolScript());
            }
        } else if (subToolType != null) {
            if (request.getImplPath() != null) {
                validateImplPath(request.getImplPath(),
                        request.getToolType() != null ? request.getToolType() : entity.getToolType(),
                        request.getAuthConfig());
                entity.setImplPath(request.getImplPath());
            }
        } else {
            if (request.getImplPath() != null) {
                validateImplPath(request.getImplPath(),
                        request.getToolType() != null ? request.getToolType() : entity.getToolType(),
                        request.getAuthConfig());
                entity.setImplPath(request.getImplPath());
            }
        }

        if (subToolType != null) {
            entity.setSubToolType(subToolType);
        }
        if (request.getAuthConfig() != null) {
            entity.setAuthConfig(request.getAuthConfig());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        toolConfigMapper.updateById(entity);
        eventPublisher.publishEvent(new ToolChangedEvent(this, IdConverter.toString(id)));
        return toDTO(entity);
    }

    @Override
    public void delete(Long id) {
        ToolConfig entity = toolConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        toolConfigMapper.deleteById(id);
        eventPublisher.publishEvent(new ToolChangedEvent(this, IdConverter.toString(id)));
    }

    @Override
    public ToolDetailDTO toggleStatus(Long id, CommonStatus status) {
        ToolConfig entity = toolConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        entity.setStatus(status);
        toolConfigMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public ToolDetailDTO getImplByName(String name) {
        LambdaQueryWrapper<ToolConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToolConfig::getUserId, UserContextUtil.requireUserId());
        wrapper.eq(ToolConfig::getName, name);
        ToolConfig entity = toolConfigMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    public ToolConfig getToolConfigBySessionAndName(Long sessionId, String toolName) {
        ToolConfigDTO dto = toolManager.getToolConfig(IdConverter.toString(sessionId), toolName);
        if (dto == null || dto.getId() == null || isKnowledgeToolId(dto.getId())) {
            return null;
        }
        ToolConfig entity = toolConfigMapper.selectById(IdConverter.parse(dto.getId()));
        if (entity == null) {
            throw new BusinessException(ErrorCode.TOOL_NOT_FOUND);
        }
        return entity;
    }

    private boolean isKnowledgeToolId(String id) {
        return KnowledgeBaseInfoTool.TOOL_NAME.equals(id)
                || KnowledgeFileInfoTool.TOOL_NAME.equals(id)
                || KnowledgeSearchTool.TOOL_NAME.equals(id)
                || KnowledgeFileChunkTool.TOOL_NAME.equals(id);
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

    private void validateToolScript(String toolScript) {
        if (toolScript == null || toolScript.isBlank()) {
            throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                    "toolScript 不能为空");
        }
        String stripped = toolScript.strip()
                .replaceAll("//[^\n]*", "")
                .replaceAll("/\\*[\\s\\S]*?\\*/", "")
                .strip();
        if (stripped.isEmpty()) {
            throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                    "toolScript 不能仅包含空白和注释");
        }
        validateJsSyntax(stripped);
    }

    private void validateJsSyntax(String script) {
        Deque<Character> stack = new ArrayDeque<>();
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (c == '\'' && !inDouble) { inSingle = !inSingle; continue; }
            if (c == '"' && !inSingle) { inDouble = !inDouble; continue; }
            if (inSingle || inDouble) continue;

            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')') {
                if (stack.isEmpty() || stack.pop() != '(')
                    throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                            "toolScript 语法错误：括号不匹配");
            } else if (c == ']') {
                if (stack.isEmpty() || stack.pop() != '[')
                    throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                            "toolScript 语法错误：括号不匹配");
            } else if (c == '}') {
                if (stack.isEmpty() || stack.pop() != '{')
                    throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                            "toolScript 语法错误：括号不匹配");
            }
        }
        if (!stack.isEmpty())
            throw new BusinessException(ErrorCode.TOOL_SCHEMA_INVALID,
                    "toolScript 语法错误：括号不匹配");
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
                        log.debug("解析 MCP 认证配置失败，使用无认证连接: {} - {}", implPath, e.getMessage());
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
        wrapper.eq(ToolConfig::getUserId, UserContextUtil.requireUserId());
        wrapper.eq(ToolConfig::getName, name);
        if (excludeId != null) {
            wrapper.ne(ToolConfig::getId, excludeId);
        }
        if (toolConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.TOOL_ALREADY_EXISTS);
        }
    }

    private ToolDetailDTO toDTO(ToolConfig entity) {
        return ToolDetailDTO.builder()
                .id(IdConverter.toString(entity.getId()))
                .name(entity.getName())
                .toolType(entity.getToolType())
                .description(entity.getDescription())
                .parameterSchema(entity.getParameterSchema())
                .returnSchema(entity.getReturnSchema())
                .implPath(entity.getImplPath())
                .authConfig(entity.getAuthConfig())
                .subToolType(entity.getSubToolType())
                .toolScript(entity.getToolScript())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
