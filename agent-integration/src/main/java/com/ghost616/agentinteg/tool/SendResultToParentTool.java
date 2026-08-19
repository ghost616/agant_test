package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class SendResultToParentTool extends CustomToolInvoker {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    public static final String TOOL_NAME = "send_result_to_parent";

    public SendResultToParentTool(ToolConfigDTO toolConfig) {
        super(toolConfig);
    }

    public static ToolConfigDTO createToolConfig() {
        return ToolConfigDTO.builder()
                .id(null)
                .toolType(ToolType.CUSTOM)
                .name(TOOL_NAME)
                .description("向父会话发送子会话执行结果")
                .parameterSchema("""
                        {
                          "type": "object",
                          "properties": {
                            "result": { "type": "string", "description": "子会话执行结果" }
                          },
                          "required": ["result"]
                        }""")
                .build();
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = JSON_MAPPER.readTree(arguments);
            JsonNode resultNode = root.get("result");
            if (resultNode == null || resultNode.isNull() || resultNode.asText().isBlank()) {
                return buildError("缺少 result 参数");
            }
            String result = resultNode.asText();

            String parentSessionId = ctx.getParentSessionId();
            if (parentSessionId == null || parentSessionId.isBlank()) {
                return buildError("当前会话不是子会话，无法发送结果到父会话");
            }

            ctx.sendUserMessage(parentSessionId, result, ctx.getModelId(), null);

            return JSON_MAPPER.writeValueAsString(Map.of("status", "success", "message", "已发送执行结果到父会话"));
        } catch (Exception e) {
            log.error("send_result_to_parent 执行失败", e);
            return buildError(e.getMessage());
        }
    }

    private static String buildError(String message) {
        try {
            return JSON_MAPPER.writeValueAsString(
                    Map.of("status", "error", "errMsg", message == null ? "未知错误" : message));
        } catch (Exception e) {
            log.error("构建错误 JSON 失败", e);
            return "{\"status\":\"error\",\"errMsg\":\"未知错误\"}";
        }
    }
}
