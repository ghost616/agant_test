package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvokerProvider;

public class DefaultCustomToolInvokerProvider implements CustomToolInvokerProvider {

    @Override
    public CustomToolInvoker getInvoker(ToolConfigDTO toolConfig) {
        throw new UnsupportedOperationException();
    }
}
