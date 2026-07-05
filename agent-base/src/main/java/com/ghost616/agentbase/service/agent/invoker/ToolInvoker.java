package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;

public interface ToolInvoker {

    String execute(AgentExecutionContext ctx, String arguments);
}
