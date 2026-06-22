package com.ghost616.platform.service.agent.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;

import java.util.HashMap;
import java.util.Map;

public final class McpAuthConfigParser {

    private McpAuthConfigParser() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> parse(String authConfig) {
        Map<String, String> result = new HashMap<>();
        if (authConfig == null || authConfig.isBlank()) {
            return result;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = mapper.readValue(authConfig, Map.class);
            String type = (String) config.get("type");
            if ("bearer".equalsIgnoreCase(type)) {
                String token = (String) config.get("token");
                if (token != null && !token.isBlank()) {
                    result.put("Authorization", "Bearer " + token);
                }
            } else if ("apikey".equalsIgnoreCase(type)) {
                String header = (String) config.get("header");
                String value = (String) config.get("value");
                if (header != null && !header.isBlank() && value != null) {
                    result.put(header, value);
                }
            } else {
                throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                        "不支持的 MCP 认证类型: " + type);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                    "解析 MCP 认证配置失败: " + e.getMessage());
        }
        return result;
    }
}
