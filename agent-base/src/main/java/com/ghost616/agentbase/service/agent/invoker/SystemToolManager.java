package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.model.ToolDefinition;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SystemToolManager {

    private final AgentComponentRegistry registry;
    private SystemToolProvider provider;

    private final Map<String, SystemTool> systemTools = new HashMap<>();
    private volatile boolean initialized;

    public SystemToolManager(AgentComponentRegistry registry) {
        this.registry = registry;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    provider = registry.getSystemToolProvider();
                    initSystemTools();
                    initialized = true;
                }
            }
        }
    }

    private void initSystemTools() {
        Map<String, SystemTool> tools = provider.discoverSystemTools();
        for (SystemTool tool : tools.values()) {
            String toolName = tool.getToolName();
            if (toolName == null || toolName.isBlank()) {
                log.debug("SystemTool {} 的 toolName 为空，跳过注册", tool.getClass().getName());
                continue;
            }
            systemTools.put(toolName, tool);
            log.debug("注册系统工具: {}", toolName);
        }
    }

    public SystemTool getSystemTool(String name) {
        ensureInitialized();
        return systemTools.get(name);
    }

    public List<ToolDefinition> getToolDefinitions() {
        ensureInitialized();
        List<ToolDefinition> definitions = new ArrayList<>();
        for (Map.Entry<String, SystemTool> entry : systemTools.entrySet()) {
            SystemTool tool = entry.getValue();
            ToolConfigDTO dto = ToolConfigDTO.builder()
                    .name("_sys_" + tool.getToolName())
                    .description(tool.getDescription())
                    .parameterSchema(tool.getParameterSchema())
                    .build();
            definitions.add(ToolDefinitionConverter.convert(dto));
        }
        return definitions;
    }
}
