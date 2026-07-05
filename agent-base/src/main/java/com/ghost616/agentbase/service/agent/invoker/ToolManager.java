package com.ghost616.agentbase.service.agent.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.McpExpandedToolDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.event.ToolChangedEvent;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ToolManager {

    private final ToolDataProvider dataProvider;

    private final ConcurrentHashMap<Long, List<ToolSessionObject>> sessionToolCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ToolSessionObject> toolCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Long>> sessionSkillToolIdsCache = new ConcurrentHashMap<>();

    public ToolManager(ToolDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public ToolInvoker getInvoker(Long sessionId, String toolName) {
        List<ToolSessionObject> sessionTools = getSessionTools(sessionId);
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
        return sessionToolCache.computeIfAbsent(sessionId, id -> {
            List<Long> toolIds = dataProvider.getSessionToolIds(id);

            List<ToolSessionObject> result = new ArrayList<>();
            for (Long toolId : toolIds) {
                ToolSessionObject tso = toolCache.computeIfAbsent(toolId,
                        tid -> buildToolSessionObject(dataProvider.getToolById(tid)));
                result.addAll(expandToolSession(tso));
            }
            return result;
        });
    }

    private ToolInvoker resolveSkillToolByName(Long sessionId, String toolName) {
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

    private ToolInvoker createInvoker(ToolConfigDTO toolConfig) {
        switch (toolConfig.getToolType()) {
            case JAVA:
                return new JavaToolInvoker(toolConfig.getImplPath());
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

    private List<McpExpandedToolDTO> expandMcpTools(ToolConfigDTO mcpConfig) {
        Map<String, String> headers;
        try {
            headers = McpAuthConfigParser.parse(mcpConfig.getAuthConfig());
        } catch (Exception e) {
            log.warn("解析 MCP 认证配置失败，跳过: {}", mcpConfig.getAuthConfig(), e);
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
                    expanded.add(McpExpandedToolDTO.builder()
                            .name(mcpConfig.getName() + "_" + tool.get("name"))
                            .toolType(ToolType.MCP_HTTP)
                            .description((String) tool.get("description"))
                            .parameterSchema(mapper.writeValueAsString(tool.get("inputSchema")))
                            .implPath(mcpConfig.getImplPath())
                            .authConfig(mcpConfig.getAuthConfig())
                            .remoteToolName((String) tool.get("name"))
                            .build());
                } catch (Exception e) {
                    log.warn("序列化 MCP 工具 Schema 失败: {}", tool.get("name"), e);
                }
            }
            log.info("MCP 工具展开结果: url={}, 展开数量={}", mcpConfig.getImplPath(), expanded.size());
            return expanded;
        } catch (Exception e) {
            log.warn("获取 MCP 工具列表失败: url={}", mcpConfig.getImplPath(), e);
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

    public record ToolSessionObject(ToolConfigDTO toolConfig, ToolInvoker invoker,
                                    ToolConfigDTO mcpOriginalConfig,
                                    List<McpExpandedToolDTO> mcpExpandedTools,
                                    List<ToolInvoker> mcpExpandedInvokers) {
    }
}
