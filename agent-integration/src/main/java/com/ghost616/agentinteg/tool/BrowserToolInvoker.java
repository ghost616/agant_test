package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class BrowserToolInvoker extends CustomToolInvoker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_JS_PATH = "browser/browser_tool_executor.js";

    private final BrowserToolCallback callback;
    private String jsContent;

    public BrowserToolInvoker(ToolConfigDTO toolConfig, BrowserToolCallback callback) {
        super(toolConfig);
        this.callback = callback;
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            String sessionId = String.valueOf(ctx.getSessionId());
            String toolId = String.valueOf(toolConfig.getId());
            String toolName = toolConfig.getName();
            return callback.execute(sessionId, toolId, toolName, arguments);
        } catch (Exception e) {
            log.error("BrowserToolInvoker execute failed", e);
            try {
                return OBJECT_MAPPER.writeValueAsString(Map.of("error", e.getMessage()));
            } catch (Exception inner) {
                return "{\"error\":\"" + inner.getMessage() + "\"}";
            }
        }
    }

    public String loadJsContent() {
        if (jsContent != null) {
            return jsContent;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_JS_PATH)) {
            if (is == null) {
                log.warn("JS file not found at {}", DEFAULT_JS_PATH);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                jsContent = reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.error("Failed to load JS file: {}", DEFAULT_JS_PATH, e);
        }
        return jsContent;
    }

    public String getJsContent() {
        return jsContent;
    }
}
