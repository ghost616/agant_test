package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;

public abstract class CustomToolInvoker implements ToolInvoker {

    protected final ToolConfigDTO toolConfig;

    protected CustomToolInvoker(ToolConfigDTO toolConfig) {
        this.toolConfig = toolConfig;
    }
}
