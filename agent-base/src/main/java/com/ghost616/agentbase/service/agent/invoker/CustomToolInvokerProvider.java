package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;

public interface CustomToolInvokerProvider {

    CustomToolInvoker getInvoker(ToolConfigDTO toolConfig);
}
