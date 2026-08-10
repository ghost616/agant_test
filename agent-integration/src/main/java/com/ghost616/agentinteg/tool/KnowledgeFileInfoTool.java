package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentinteg.knowledge.FileInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class KnowledgeFileInfoTool extends CustomToolInvoker {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    public static final String TOOL_NAME = "default_tool_rag_file_info";
    private static final int DEFAULT_SEARCH_LIMIT = 10;

    private final KnowledgeBaseQueryProvider provider;

    public KnowledgeFileInfoTool(ToolConfigDTO toolConfig, KnowledgeBaseQueryProvider provider) {
        super(toolConfig);
        this.provider = provider;
    }

    public static ToolConfigDTO createToolConfig() {
        return ToolConfigDTO.builder()
                .id(null)
                .toolType(ToolType.CUSTOM)
                .name(TOOL_NAME)
                .description("搜索知识库下的文件信息，支持按文件名关键字过滤")
                .parameterSchema("""
                        {
                          "type": "object",
                          "properties": {
                            "knowledgeBaseId": { "type": "string", "description": "知识库 ID" },
                            "fileId": { "type": "string", "description": "文件 ID，可选" },
                            "fileName": { "type": "string", "description": "文件名关键字" },
                            "searchLimit": { "type": "integer", "description": "返回数量上限，默认 10" }
                          },
                          "required": ["knowledgeBaseId"]
                        }""")
                .build();
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = JSON_MAPPER.readTree(arguments);
            JsonNode kbIdNode = root.get("knowledgeBaseId");
            if (kbIdNode == null || kbIdNode.isNull() || kbIdNode.asText().isBlank()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 knowledgeBaseId 参数\"}";
            }
            String knowledgeBaseId = kbIdNode.asText();
            String fileId = root.has("fileId") && !root.get("fileId").isNull()
                    && !root.get("fileId").asText().isBlank()
                    ? root.get("fileId").asText() : null;
            String fileName = root.has("fileName") && !root.get("fileName").isNull()
                    ? root.get("fileName").asText() : null;
            int searchLimit = root.has("searchLimit") && root.get("searchLimit").canConvertToInt()
                    ? root.get("searchLimit").asInt() : DEFAULT_SEARCH_LIMIT;

            List<FileInfo> files = provider.searchFiles(knowledgeBaseId, fileName, searchLimit);
            if (fileId != null && files != null) {
                files = files.stream().filter(f -> fileId.equals(f.fileId())).toList();
            }
            return JSON_MAPPER.writeValueAsString(files);
        } catch (Exception e) {
            log.error("default_tool_rag_file_info 执行失败", e);
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
