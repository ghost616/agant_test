package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;

public interface ConfigurableToolInvoker extends ToolInvoker {

    void setToolConfig(ToolConfigDTO toolConfig);
}
