package com.ghost616.agentinteg.tool;

@FunctionalInterface
public interface BrowserToolCallback {

    String execute(String sessionId, String toolId, String toolName, String toolParams);
}
