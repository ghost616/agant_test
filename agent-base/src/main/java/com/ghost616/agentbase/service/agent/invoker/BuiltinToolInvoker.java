package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;

public class BuiltinToolInvoker implements ToolInvoker {

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        return arguments;
    }
}
