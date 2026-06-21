package com.ghost616.platform.service.agent.invoker;

import com.ghost616.platform.service.agent.AgentExecutionContext;

public interface ToolInvoker {

    String execute(AgentExecutionContext ctx, String arguments);
}
