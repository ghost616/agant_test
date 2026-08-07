package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentinteg.knowledge.FileInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeFileInfoTool implements SystemTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TOOL_NAME = "kb_file_info";
    private static final int DEFAULT_SEARCH_LIMIT = 10;

    private final KnowledgeBaseQueryProvider provider;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "搜索知识库下的文件信息，支持按文件名关键字过滤";
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "knowledgeBaseId": { "type": "integer", "description": "知识库 ID" },
                    "fileName": { "type": "string", "description": "文件名关键字" },
                    "searchLimit": { "type": "integer", "description": "返回数量上限，默认 10" }
                  },
                  "required": ["knowledgeBaseId"]
                }""";
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(arguments);
            JsonNode kbIdNode = root.get("knowledgeBaseId");
            if (kbIdNode == null || kbIdNode.isNull() || !kbIdNode.canConvertToLong()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 knowledgeBaseId 参数\"}";
            }
            Long knowledgeBaseId = kbIdNode.asLong();
            String fileName = root.has("fileName") && !root.get("fileName").isNull()
                    ? root.get("fileName").asText() : null;
            int searchLimit = root.has("searchLimit") && root.get("searchLimit").canConvertToInt()
                    ? root.get("searchLimit").asInt() : DEFAULT_SEARCH_LIMIT;

            List<FileInfo> files = provider.searchFiles(knowledgeBaseId, fileName, searchLimit);
            return OBJECT_MAPPER.writeValueAsString(files);
        } catch (Exception e) {
            log.error("kb_file_info 执行失败", e);
            return buildError(e.getMessage());
        }
    }

    private static String buildError(String message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(
                    Map.of("status", "error", "errMsg", message == null ? "未知错误" : message));
        } catch (Exception e) {
            log.error("构建错误 JSON 失败", e);
            return "{\"status\":\"error\",\"errMsg\":\"未知错误\"}";
        }
    }
}
