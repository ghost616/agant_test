package com.ghost616.agentinteg.tool;

@FunctionalInterface
public interface BrowserToolProvider {

    String execute(String sessionId, String toolConfigId, String toolName, String toolParams);
}
