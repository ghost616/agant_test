package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentinteg.knowledge.FileInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeFileChunkTool implements SystemTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TOOL_NAME = "kb_file_chunk";
    private static final int DEFAULT_START_LINE = 0;
    private static final int MAX_FILE_LOOKUP_LIMIT = 1000;

    private final KnowledgeBaseQueryProvider provider;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "获取知识库文件中指定行号范围内的文本块，不传 endLine 时默认为文件最大行数";
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "knowledgeBaseId": { "type": "integer", "description": "知识库 ID" },
                    "fileId": { "type": "integer", "description": "文件 ID" },
                    "startLine": { "type": "integer", "description": "起始行号，默认 0" },
                    "endLine": { "type": "integer", "description": "结束行号，默认文件最大行数" }
                  },
                  "required": ["knowledgeBaseId", "fileId"]
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
            JsonNode fileIdNode = root.get("fileId");
            if (fileIdNode == null || fileIdNode.isNull() || !fileIdNode.canConvertToLong()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 fileId 参数\"}";
            }
            Long knowledgeBaseId = kbIdNode.asLong();
            Long fileId = fileIdNode.asLong();
            int startLine = root.has("startLine") && root.get("startLine").canConvertToInt()
                    ? root.get("startLine").asInt() : DEFAULT_START_LINE;
            int endLine = root.has("endLine") && root.get("endLine").canConvertToInt()
                    ? root.get("endLine").asInt() : resolveFileMaxLine(knowledgeBaseId, fileId);

            TextChunkWithFile result = provider.getFileChunks(knowledgeBaseId, fileId, startLine, endLine);
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            log.error("kb_file_chunk 执行失败", e);
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

    private int resolveFileMaxLine(Long knowledgeBaseId, Long fileId) {
        List<FileInfo> files = provider.searchFiles(knowledgeBaseId, null, MAX_FILE_LOOKUP_LIMIT);
        if (files != null) {
            for (FileInfo file : files) {
                if (file.fileId().equals(fileId)) {
                    return file.maxLineCount();
                }
            }
        }
        return Integer.MAX_VALUE;
    }
}
