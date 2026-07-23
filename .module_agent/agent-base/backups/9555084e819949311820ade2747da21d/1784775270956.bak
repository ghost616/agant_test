package com.ghost616.agentbase.service.agent.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.event.ToolChangedEvent;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SessionToolInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ToolManager {

    private final AgentComponentRegistry registry;
    private ToolDataProvider dataProvider;
    private AgentContextManager agentContextManager;

    private final ConcurrentHashMap<Long, List<ToolSessionObject>> sessionToolCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ToolSessionObject> toolCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Long>> sessionSkillToolIdsCache = new ConcurrentHashMap<>();
    private volatile boolean initialized;

    public ToolManager(AgentComponentRegistry registry) {
        this.registry = registry;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    dataProvider = registry.getToolDataProvider();
                    agentContextManager = registry.getAgentContextManager();
                    initialized = true;
                }
            }
        }
    }

    private boolean isSubSession(Long sessionId) {
        AgentContextManager.AgentSessionContext ctx = agentContextManager.get(sessionId);
        return ctx != null && !ctx.context().isMainSession();
    }

    public ToolInvoker getInvoker(Long sessionId, String toolName) {
        ensureInitialized();
        List<ToolSessionObject> sessionTools = getSessionTools(sessionId, isSubSession(sessionId));
        ToolInvoker invoker = sessionTools.stream()
                .filter(tso -> tso.toolConfig().getName().equals(toolName))
                .map(ToolSessionObject::invoker)
                .findFirst()
                .orElse(null);
        if (invoker != null) {
            return invoker;
        }
        return resolveSkillToolByName(sessionId, toolName);
    }

    public List<ToolSessionObject> getSessionTools(Long sessionId) {
        return getSessionTools(sessionId, false);
    }

    public List<ToolSessionObject> getSessionTools(Long sessionId, boolean expandChildMcp) {
        ensureInitialized();
        return sessionToolCache.computeIfAbsent(sessionId, id -> {
            List<SessionToolInfo> sessionToolInfos = dataProvider.getSessionToolIds(id);

            List<ToolSessionObject> result = new ArrayList<>();
            for (SessionToolInfo info : sessionToolInfos) {
                Long toolId = info.toolId();
                ToolSessionObject tso = toolCache.computeIfAbsent(toolId,
                        tid -> buildToolSessionObject(dataProvider.getToolById(tid)));
                boolean isOriginalMcp = tso.toolConfig().getToolType() == ToolType.MCP_HTTP;
                List<ToolSessionObject> expanded = expandToolSession(tso);
                for (ToolSessionObject etso : expanded) {
                    if (!expandChildMcp) {
                        if (isOriginalMcp) {
                            ToolConfigDTO parentCopy = copyToolConfig(etso.toolConfig());
                            parentCopy.setSessionAuth(SessionAuthType.PARENT);
                            result.add(new ToolSessionObject(parentCopy, etso.invoker(),
                                    etso.mcpOriginalConfig(), List.of(), List.of()));
                        } else {
                            ToolConfigDTO copy = copyToolConfig(etso.toolConfig());
                            copy.setSessionAuth(info.sessionAuth());
                            result.add(new ToolSessionObject(copy, etso.invoker(),
                                    etso.mcpOriginalConfig(), List.of(), List.of()));
                        }
                    } else {
                        result.add(etso);
                    }
                }
                if (!expandChildMcp && isOriginalMcp && info.sessionAuth() == SessionAuthType.ALL) {
                    ToolConfigDTO childCopy = copyToolConfig(tso.toolConfig());
                    childCopy.setSessionAuth(SessionAuthType.CHILD);
                    result.add(new ToolSessionObject(childCopy, null, childCopy, List.of(), List.of()));
                }
            }
            if (expandChildMcp) {
                result = expandChildMcpTools(result);
                List<ToolSessionObject> processed = new ArrayList<>();
                for (ToolSessionObject tso : result) {
                    ToolConfigDTO copy = copyToolConfig(tso.toolConfig());
                    copy.setSessionAuth(SessionAuthType.PARENT);
                    processed.add(new ToolSessionObject(copy, tso.invoker(),
                            tso.mcpOriginalConfig(), List.of(), List.of()));
                }
                result = processed;
            }
            return result;
        });
    }

    private ToolInvoker resolveSkillToolByName(Long sessionId, String toolName) {
        ensureInitialized();
        List<Long> toolIds = sessionSkillToolIdsCache.computeIfAbsent(sessionId,
                dataProvider::getSkillToolIds);

        if (toolIds.isEmpty()) {
            return null;
        }

        for (Long toolId : toolIds) {
            ToolSessionObject tso = toolCache.computeIfAbsent(toolId,
                    id -> buildToolSessionObject(dataProvider.getToolById(id)));
            ToolConfigDTO dto = tso.toolConfig();
            if (!tso.mcpExpandedTools().isEmpty()) {
                for (int i = 0; i < tso.mcpExpandedTools().size(); i++) {
                    if (tso.mcpExpandedTools().get(i).getName().equals(toolName)) {
                        return tso.mcpExpandedInvokers().get(i);
                    }
                }
            } if (dto.getName().equals(toolName)) {
                return tso.invoker();
            }
        }

        return null;
    }

    private ToolSessionObject buildToolSessionObject(ToolConfigDTO dto) {
        if (dto.getToolType() == ToolType.MCP_HTTP) {
            if (dto.getSessionAuth() == SessionAuthType.CHILD) {
                return new ToolSessionObject(dto, null, dto, List.of(), List.of());
            }
            List<McpExpandedToolDTO> expandedTools = expandMcpTools(dto);
            List<ToolInvoker> expandedInvokers = expandedTools.stream()
                    .map(this::createInvoker)
                    .toList();
            return new ToolSessionObject(dto, null, dto, expandedTools, expandedInvokers);
        }
        return new ToolSessionObject(dto, createInvoker(dto), null, List.of(), List.of());
    }

    private List<ToolSessionObject> expandToolSession(ToolSessionObject tso) {
        if (tso.mcpExpandedTools().isEmpty()) {
            return List.of(tso);
        }
        List<ToolSessionObject> flattened = new ArrayList<>();
        for (int i = 0; i < tso.mcpExpandedTools().size(); i++) {
            McpExpandedToolDTO expandedTool = tso.mcpExpandedTools().get(i);
            ToolInvoker expandedInvoker = tso.mcpExpandedInvokers().get(i);
            flattened.add(new ToolSessionObject(
                    expandedTool, expandedInvoker, tso.mcpOriginalConfig(),
                    List.of(), List.of()));
        }
        return flattened;
    }

    private List<ToolSessionObject> expandChildMcpTools(List<ToolSessionObject> tools) {
        List<ToolSessionObject> expanded = new ArrayList<>();
        for (ToolSessionObject tso : tools) {
            if (tso.mcpExpandedTools().isEmpty()
                    && tso.toolConfig().getToolType() == ToolType.MCP_HTTP
                    && tso.toolConfig().getSessionAuth() == SessionAuthType.CHILD) {
                ToolConfigDTO originalConfig = tso.mcpOriginalConfig();
                List<McpExpandedToolDTO> expandedMcps = expandMcpTools(originalConfig);
                for (McpExpandedToolDTO et : expandedMcps) {
                    ToolInvoker invoker = createInvoker(et);
                    expanded.add(new ToolSessionObject(et, invoker, originalConfig, List.of(), List.of()));
                }
            } else {
                expanded.add(tso);
            }
        }
        return expanded;
    }

    private ToolInvoker createInvoker(ToolConfigDTO toolConfig) {
        switch (toolConfig.getToolType()) {
            case JAVA:
                return new JavaToolInvoker(toolConfig.getImplPath(), toolConfig);
            case TYPESCRIPT:
                return new TypeScriptToolInvoker(toolConfig.getImplPath());
            case PYTHON:
                return new PythonToolInvoker(toolConfig.getImplPath());
            case MCP_HTTP:
                String remoteName = toolConfig instanceof McpExpandedToolDTO mcp
                        ? mcp.getRemoteToolName()
                        : toolConfig.getName();
                return new McpHttpToolInvoker(toolConfig.getImplPath(), remoteName,
                        toolConfig.getAuthConfig());
            default:
                throw new UnsupportedOperationException("暂不支持的调用类型: " + toolConfig.getToolType());
        }
    }

    public List<McpExpandedToolDTO> expandMcpTools(ToolConfigDTO mcpConfig) {
        Map<String, String> headers;
        try {
            headers = McpAuthConfigParser.parse(mcpConfig.getAuthConfig());
        } catch (Exception e) {
            log.debug("解析 MCP 认证配置失败，跳过: {}", mcpConfig.getAuthConfig(), e);
            return List.of();
        }
        try {
            McpJsonRpcClient client = new McpJsonRpcClient(mcpConfig.getImplPath(), headers);
            client.initialize();
            List<Map<String, Object>> tools = client.listTools();
            ObjectMapper mapper = new ObjectMapper();
            List<McpExpandedToolDTO> expanded = new ArrayList<>();
            for (Map<String, Object> tool : tools) {
                try {
                    String remoteName = (String) tool.get("name");
                    String prefixedName = mcpConfig.getName() + "_" + remoteName;
                    String description = (String) tool.get("description");
                    if (description != null) {
                        description = description.replace(remoteName, prefixedName);
                    }
                    expanded.add(McpExpandedToolDTO.builder()
                            .id(mcpConfig.getId())
                            .name(prefixedName)
                            .toolType(ToolType.MCP_HTTP)
                            .description(description)
                            .parameterSchema(mapper.writeValueAsString(tool.get("inputSchema")))
                            .implPath(mcpConfig.getImplPath())
                            .authConfig(mcpConfig.getAuthConfig())
                            .sessionAuth(mcpConfig.getSessionAuth())
                            .remoteToolName(remoteName)
                            .build());
                } catch (Exception e) {
                    log.debug("序列化 MCP 工具 Schema 失败: {}", tool.get("name"), e);
                }
            }
            log.debug("MCP 工具展开结果: url={}, 展开数量={}", mcpConfig.getImplPath(), expanded.size());
            return expanded;
        } catch (Exception e) {
            log.debug("获取 MCP 工具列表失败: url={}", mcpConfig.getImplPath(), e);
            return List.of();
        }
    }

    public String execute(ToolInvoker invoker, AgentExecutionContext context, String arguments) {
        return invoker.execute(context, arguments);
    }

    public void clearSessionCache(Long sessionId) {
        sessionToolCache.remove(sessionId);
        sessionSkillToolIdsCache.remove(sessionId);
    }

    public void clearToolCache(Long toolId) {
        toolCache.remove(toolId);
    }

    @EventListener
    public void onToolChanged(ToolChangedEvent event) {
        clearToolCache(event.getToolId());
    }

    private ToolConfigDTO copyToolConfig(ToolConfigDTO original) {
        if (original instanceof McpExpandedToolDTO mcp) {
            return McpExpandedToolDTO.builder()
                    .id(mcp.getId())
                    .name(mcp.getName())
                    .toolType(mcp.getToolType())
                    .description(mcp.getDescription())
                    .parameterSchema(mcp.getParameterSchema())
                    .returnSchema(mcp.getReturnSchema())
                    .implPath(mcp.getImplPath())
                    .authConfig(mcp.getAuthConfig())
                    .status(mcp.getStatus())
                    .sessionAuth(mcp.getSessionAuth())
                    .createTime(mcp.getCreateTime())
                    .updateTime(mcp.getUpdateTime())
                    .remoteToolName(mcp.getRemoteToolName())
                    .build();
        }
        return ToolConfigDTO.builder()
                .id(original.getId())
                .name(original.getName())
                .toolType(original.getToolType())
                .description(original.getDescription())
                .parameterSchema(original.getParameterSchema())
                .returnSchema(original.getReturnSchema())
                .implPath(original.getImplPath())
                .authConfig(original.getAuthConfig())
                .status(original.getStatus())
                .sessionAuth(original.getSessionAuth())
                .createTime(original.getCreateTime())
                .updateTime(original.getUpdateTime())
                .build();
    }

    public record ToolSessionObject(ToolConfigDTO toolConfig, ToolInvoker invoker,
                                    ToolConfigDTO mcpOriginalConfig,
                                    List<McpExpandedToolDTO> mcpExpandedTools,
                                    List<ToolInvoker> mcpExpandedInvokers) {
    }
}
