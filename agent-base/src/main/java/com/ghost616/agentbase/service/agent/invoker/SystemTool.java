package com.ghost616.agentbase.service.agent.invoker;

public interface SystemTool extends ToolInvoker {

    String getToolName();

    String getDescription();

    String getParameterSchema();
}
