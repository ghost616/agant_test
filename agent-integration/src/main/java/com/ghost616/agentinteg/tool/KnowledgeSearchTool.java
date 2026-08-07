package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import com.ghost616.agentinteg.knowledge.SearchType;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile.TextChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSearchTool implements SystemTool {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final String TOOL_NAME = "kb_search";
    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int DEFAULT_CONTEXT_LINES = 3;

    private final KnowledgeBaseQueryProvider provider;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "搜索知识库文本块，按上下文行数扩展并合并连续行号块后返回";
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "knowledgeBaseId": { "type": "integer", "description": "知识库 ID" },
                    "fileId": { "type": "integer", "description": "文件 ID，不传表示不限文件" },
                    "searchType": { "type": "string", "enum": ["VECTOR", "FULLTEXT", "HYBRID"], "description": "搜索类型" },
                    "query": { "type": "string", "description": "查询关键字" },
                    "searchLimit": { "type": "integer", "description": "返回数量上限，默认 10" },
                    "contextLines": { "type": "integer", "description": "上下文行数，默认 3" }
                  },
                  "required": ["knowledgeBaseId", "searchType", "query"]
                }""";
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = JSON_MAPPER.readTree(arguments);
            JsonNode kbIdNode = root.get("knowledgeBaseId");
            if (kbIdNode == null || kbIdNode.isNull() || !kbIdNode.canConvertToLong()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 knowledgeBaseId 参数\"}";
            }
            Long knowledgeBaseId = kbIdNode.asLong();
            Long fileId = root.has("fileId") && root.get("fileId").canConvertToLong()
                    ? root.get("fileId").asLong() : null;
            String searchTypeText = root.has("searchType") && !root.get("searchType").isNull()
                    ? root.get("searchType").asText() : null;
            String query = root.has("query") && !root.get("query").isNull()
                    ? root.get("query").asText() : null;
            if (searchTypeText == null || searchTypeText.isBlank() || query == null || query.isBlank()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 searchType 或 query 参数\"}";
            }
            SearchType searchType = parseSearchType(searchTypeText);
            if (searchType == null) {
                return "{\"status\":\"error\",\"errMsg\":\"无效的 searchType: " + searchTypeText + "\"}";
            }
            int searchLimit = root.has("searchLimit") && root.get("searchLimit").canConvertToInt()
                    ? root.get("searchLimit").asInt() : DEFAULT_SEARCH_LIMIT;
            int contextLines = root.has("contextLines") && root.get("contextLines").canConvertToInt()
                    ? root.get("contextLines").asInt() : DEFAULT_CONTEXT_LINES;

            List<TextChunkWithFile> results = provider.searchChunks(
                    knowledgeBaseId, fileId, searchType, query, searchLimit, contextLines);

            List<Map<String, Object>> output = new ArrayList<>();
            if (results != null) {
                for (TextChunkWithFile withFile : results) {
                    output.add(buildResult(withFile));
                }
            }
            return JSON_MAPPER.writeValueAsString(output);
        } catch (Exception e) {
            log.error("kb_search 执行失败", e);
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

    private Map<String, Object> buildResult(TextChunkWithFile withFile) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("knowledgeBaseId", withFile.knowledgeBaseId());
        result.put("fileId", withFile.fileId());
        result.put("chunks", mergeContinuousChunks(withFile.chunkList()));
        return result;
    }

    private List<TextChunk> mergeContinuousChunks(List<TextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<TextChunk> sorted = chunks.stream()
                .sorted(Comparator.comparingInt(TextChunk::lineNumber))
                .toList();
        List<TextChunk> merged = new ArrayList<>();
        int startLine = sorted.get(0).lineNumber();
        int lastLine = startLine;
        StringBuilder text = new StringBuilder(sorted.get(0).text());
        for (int i = 1; i < sorted.size(); i++) {
            TextChunk chunk = sorted.get(i);
            if (chunk.lineNumber() == lastLine + 1) {
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append(chunk.text());
                lastLine = chunk.lineNumber();
            } else {
                merged.add(new TextChunk(startLine, text.toString()));
                startLine = chunk.lineNumber();
                lastLine = startLine;
                text = new StringBuilder(chunk.text());
            }
        }
        merged.add(new TextChunk(startLine, text.toString()));
        return merged;
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
