package com.ghost616.agentbase.service.agent.invoker;

import java.util.Map;

public interface SystemToolProvider {

    Map<String, SystemTool> discoverSystemTools();
}
