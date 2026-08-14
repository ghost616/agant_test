package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentinteg.knowledge.SearchType;
import com.ghost616.agentinteg.memory.MemoryQueryProvider;
import com.ghost616.agentinteg.memory.MemoryResult;
import com.ghost616.agentinteg.memory.MessageSeqByRole;
import com.ghost616.agentinteg.memory.SeqRange;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MemoryQueryTool extends CustomToolInvoker {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    public static final String TOOL_NAME = "default_tool_memory_search";
    private static final String MEMORY_TYPE_GROUP = "GROUP";
    private static final String MEMORY_TYPE_DAILY = "DAILY";

    private final MemoryQueryProvider provider;

    public MemoryQueryTool(ToolConfigDTO toolConfig, MemoryQueryProvider provider) {
        super(toolConfig);
        this.provider = provider;
    }

    public static ToolConfigDTO createToolConfig() {
        return ToolConfigDTO.builder()
                .id(null)
                .toolType(ToolType.CUSTOM)
                .name(TOOL_NAME)
                .description("搜索 AI 记忆，返回匹配的记忆内容及对应角色的消息序号列表")
                .parameterSchema("""
                        {
                          "type": "object",
                          "properties": {
                            "query": { "type": "string", "description": "查询关键字" },
                            "searchType": { "type": "string", "enum": ["VECTOR", "FULLTEXT", "HYBRID"], "description": "搜索类型" },
                            "memoryType": { "type": "string", "enum": ["GROUP", "DAILY"], "description": "记忆类型，GROUP=分类/DAILY=按天，不传或空表示所有记忆" },
                            "startTime": { "type": "integer", "description": "起始时间（毫秒时间戳，可选）" },
                            "endTime": { "type": "integer", "description": "结束时间（毫秒时间戳，可选）" }
                          },
                          "required": ["query", "searchType"]
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

            String query = root.has("query") && !root.get("query").isNull()
                    ? root.get("query").asText() : null;
            if (query == null || query.isBlank()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 query 参数\"}";
            }

            String searchTypeText = root.has("searchType") && !root.get("searchType").isNull()
                    ? root.get("searchType").asText() : null;
            if (searchTypeText == null || searchTypeText.isBlank()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 searchType 参数\"}";
            }
            SearchType searchType = parseSearchType(searchTypeText);
            if (searchType == null) {
                return "{\"status\":\"error\",\"errMsg\":\"无效的 searchType: " + searchTypeText + "\"}";
            }

            String memoryType = root.has("memoryType") && !root.get("memoryType").isNull()
                    && !root.get("memoryType").asText().isBlank()
                    ? root.get("memoryType").asText() : null;
            if (memoryType != null && !memoryType.equals(MEMORY_TYPE_GROUP)
                    && !memoryType.equals(MEMORY_TYPE_DAILY)) {
                return "{\"status\":\"error\",\"errMsg\":\"无效的 memoryType: " + memoryType + "\"}";
            }

            Long startTime = parseLong(root, "startTime");
            if (startTime == null && root.hasNonNull("startTime")) {
                return "{\"status\":\"error\",\"errMsg\":\"无效的 startTime 参数\"}";
            }
            Long endTime = parseLong(root, "endTime");
            if (endTime == null && root.hasNonNull("endTime")) {
                return "{\"status\":\"error\",\"errMsg\":\"无效的 endTime 参数\"}";
            }

            List<MemoryResult> results = provider.getMemories(
                    sessionId, searchType, memoryType, startTime, endTime, query);

            return JSON_MAPPER.writeValueAsString(buildOutput(sessionId, results));
        } catch (Exception e) {
            log.error("default_tool_memory_search 执行失败", e);
            return buildError(e.getMessage());
        }
    }

    private SearchType parseSearchType(String text) {
        try {
            return SearchType.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Long parseLong(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.canConvertToLong()) {
            return node.asLong();
        }
        return null;
    }

    private Map<String, Object> buildOutput(String sessionId, List<MemoryResult> results) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        List<SeqRange> ranges = new ArrayList<>();

        if (results != null) {
            for (MemoryResult result : results) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("content", result.content());
                item.put("startSeq", result.startSeq());
                item.put("endSeq", result.endSeq());
                item.put("memoryType", result.memoryType());
                resultList.add(item);
                ranges.add(new SeqRange(result.startSeq(), result.endSeq()));
            }
        }

        Set<Integer> userSeqs = new LinkedHashSet<>();
        Set<Integer> toolSeqs = new LinkedHashSet<>();
        Set<Integer> assistantSeqs = new LinkedHashSet<>();
        if (!ranges.isEmpty()) {
            MessageSeqByRole byRole = provider.getMessageSeqsByRole(sessionId, ranges);
            if (byRole != null) {
                if (byRole.userSeqList() != null) {
                    userSeqs.addAll(byRole.userSeqList());
                }
                if (byRole.toolSeqList() != null) {
                    toolSeqs.addAll(byRole.toolSeqList());
                }
                if (byRole.assistantSeqList() != null) {
                    assistantSeqs.addAll(byRole.assistantSeqList());
                }
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("results", resultList);
        output.put("userSeqList", new ArrayList<>(userSeqs));
        output.put("toolSeqList", new ArrayList<>(toolSeqs));
        output.put("assistantSeqList", new ArrayList<>(assistantSeqs));
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
