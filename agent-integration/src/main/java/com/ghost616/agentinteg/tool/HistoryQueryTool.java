package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentinteg.history.HistoryMessageItem;
import com.ghost616.agentinteg.history.HistoryMessageQueryProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HistoryQueryTool extends CustomToolInvoker {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    public static final String TOOL_NAME = "default_tool_history_query";

    private final HistoryMessageQueryProvider provider;

    public HistoryQueryTool(ToolConfigDTO toolConfig, HistoryMessageQueryProvider provider) {
        super(toolConfig);
        this.provider = provider;
    }

    public static ToolConfigDTO createToolConfig() {
        return ToolConfigDTO.builder()
                .id(null)
                .toolType(ToolType.CUSTOM)
                .name(TOOL_NAME)
                .description("按消息序号查询会话历史消息，返回消息内容、推理内容及工具请求信息")
                .parameterSchema("""
                        {
                          "type": "object",
                          "properties": {
                            "seqs": { "type": "array", "items": { "type": "integer" }, "description": "消息序号列表" },
                            "includeReasoning": { "type": "boolean", "description": "是否包含推理内容，默认 false" }
                          },
                          "required": ["seqs"]
                        }""")
                .build();
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            String sessionId = ctx.getSessionId();
            if (sessionId == null || sessionId.isBlank()) {
                return "{\"status\":\"error\",\"errMsg\":\"无法获取会话 ID\"}";
            }
            JsonNode root = JSON_MAPPER.readTree(arguments);

            JsonNode seqsNode = root.get("seqs");
            if (seqsNode == null || !seqsNode.isArray() || seqsNode.isEmpty()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 seqs 参数\"}";
            }
            List<Integer> seqs = new ArrayList<>();
            for (JsonNode item : seqsNode) {
                if (!item.canConvertToInt()) {
                    return "{\"status\":\"error\",\"errMsg\":\"无效的 seqs 参数\"}";
                }
                seqs.add(item.asInt());
            }

            boolean includeReasoning = false;
            if (root.hasNonNull("includeReasoning")) {
                includeReasoning = root.get("includeReasoning").asBoolean(false);
            }

            List<HistoryMessageItem> messages = provider.getMessagesBySeqs(
                    sessionId, seqs, includeReasoning);

            return JSON_MAPPER.writeValueAsString(buildOutput(messages, includeReasoning));
        } catch (Exception e) {
            log.error("default_tool_history_query 执行失败", e);
            return buildError(e.getMessage());
        }
    }

    private Map<String, Object> buildOutput(List<HistoryMessageItem> messages, boolean includeReasoning) {
        List<Map<String, Object>> messageList = new ArrayList<>();
        if (messages != null) {
            for (HistoryMessageItem message : messages) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", message.role());
                item.put("content", message.content());
                if (includeReasoning && message.reasoning() != null) {
                    item.put("reasoning", message.reasoning());
                }
                if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                    List<Map<String, Object>> toolCalls = new ArrayList<>();
                    for (HistoryMessageItem.HistoryToolCallItem call : message.toolCalls()) {
                        Map<String, Object> callItem = new LinkedHashMap<>();
                        callItem.put("toolCallId", call.toolCallId());
                        callItem.put("toolCallName", call.toolCallName());
                        callItem.put("toolCallArguments", call.toolCallArguments());
                        toolCalls.add(callItem);
                    }
                    item.put("toolCalls", toolCalls);
                }
                if (message.toolResult() != null) {
                    Map<String, Object> toolResult = new LinkedHashMap<>();
                    toolResult.put("toolCallId", message.toolResult().toolCallId());
                    toolResult.put("toolCallName", message.toolResult().toolCallName());
                    toolResult.put("content", message.content());
                    item.put("toolResult", toolResult);
                }
                messageList.add(item);
            }
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("messages", messageList);
        return output;
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
