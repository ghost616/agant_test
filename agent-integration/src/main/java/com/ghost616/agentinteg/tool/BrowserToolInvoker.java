package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
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

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final String DEFAULT_JS_PATH = "browser/browser_tool_executor.js";

    private final BrowserToolProvider callback;
    private static String jsContent;

    public BrowserToolInvoker(ToolConfigDTO toolConfig, BrowserToolProvider callback) {
        super(toolConfig);
        this.callback = callback;
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            String sessionId = ctx.getSessionId();
            String toolConfigId = toolConfig.getId();
            String toolName = toolConfig.getName();
            return callback.execute(sessionId, toolConfigId, toolName, arguments);
        } catch (Exception e) {
            log.error("BrowserToolInvoker execute failed", e);
            try {
                return JSON_MAPPER.writeValueAsString(Map.of("status", "error", "errMsg", e.getMessage()));
            } catch (Exception inner) {
                return "{\"status\":\"error\",\"errMsg\":\"" + inner.getMessage() + "\"}";
            }
        }
    }

    public static String loadJsContent() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_JS_PATH)) {
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

    public static String getJsContent() {
        return jsContent;
    }
}
