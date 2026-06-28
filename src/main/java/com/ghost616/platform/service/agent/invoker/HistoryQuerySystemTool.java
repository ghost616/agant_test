package com.ghost616.platform.service.agent.invoker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.service.agent.AgentExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HistoryQuerySystemTool implements SystemTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getToolName() {
        return "history_query";
    }

    @Override
    public String getDescription() {
        return "展开被折叠的历史消息组，传入消息组索引列表可获取对应组的完整消息内容";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"indices\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"},\"description\":\"要展开的消息组索引列表\"}},\"required\":[\"indices\"]}";
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = MAPPER.readTree(arguments);
            JsonNode indicesNode = root.get("indices");
            if (indicesNode == null || !indicesNode.isArray()) {
                return "{\"error\":\"缺少 indices 参数\"}";
            }
            String jsonStr = indicesNode.toString();
            ctx.putConversationVariable("_sys_his_msgs_index", jsonStr);
            return "{\"status\":\"ok\",\"indices\":" + jsonStr + "}";
        } catch (Exception e) {
            log.error("history_query 执行失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
