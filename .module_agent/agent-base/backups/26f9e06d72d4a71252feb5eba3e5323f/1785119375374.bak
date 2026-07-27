package com.ghost616.agentbase.service.agent.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConversationVariableSystemTool implements SystemTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getToolName() {
        return "conversation_variable";
    }

    @Override
    public String getDescription() {
        return "管理对话变量，支持添加、获取、移除操作";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\",\"enum\":[\"add\",\"get\",\"remove\"],\"description\":\"操作类型\"},\"key\":{\"type\":\"string\",\"description\":\"变量名\"},\"value\":{\"type\":\"string\",\"description\":\"变量值（add 时必填）\"}},\"required\":[\"action\",\"key\"]}";
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = MAPPER.readTree(arguments);
            String action = root.has("action") ? root.get("action").asText() : null;
            String key = root.has("key") ? root.get("key").asText() : null;

            if (action == null || key == null || key.isBlank()) {
                return "{\"error\":\"缺少 action 或 key 参数\"}";
            }

            switch (action) {
                case "add": {
                    String value = root.has("value") ? root.get("value").asText() : null;
                    if (value == null) {
                        return "{\"error\":\"add 操作缺少 value 参数\"}";
                    }
                    ctx.putConversationVariable(key, value);
                    return "{\"status\":\"ok\",\"action\":\"add\",\"key\":\"" + key + "\"}";
                }
                case "get": {
                    String value = ctx.getConversationVariable(key);
                    return "{\"status\":\"ok\",\"action\":\"get\",\"key\":\"" + key + "\",\"value\":" + (value != null ? "\"" + MAPPER.writeValueAsString(value).replaceAll("^\"|\"$", "") + "\"" : "null") + "}";
                }
                case "remove": {
                    ctx.removeConversationVariable(key);
                    return "{\"status\":\"ok\",\"action\":\"remove\",\"key\":\"" + key + "\"}";
                }
                default:
                    return "{\"error\":\"不支持的 action: " + action + "\"}";
            }
        } catch (Exception e) {
            log.error("conversation_variable 执行失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
