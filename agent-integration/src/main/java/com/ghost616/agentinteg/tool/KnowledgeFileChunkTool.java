package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentinteg.knowledge.FileInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile;
import com.ghost616.agentinteg.knowledge.TextChunkWithFile.TextChunk;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class KnowledgeFileChunkTool extends CustomToolInvoker {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    public static final String TOOL_NAME = "default_tool_rag_file_chunk";
    private static final int DEFAULT_START_LINE = 0;
    private static final int MAX_FILE_LOOKUP_LIMIT = 1000;

    private final KnowledgeBaseQueryProvider provider;

    public KnowledgeFileChunkTool(ToolConfigDTO toolConfig, KnowledgeBaseQueryProvider provider) {
        super(toolConfig);
        this.provider = provider;
    }

    public static ToolConfigDTO createToolConfig() {
        return ToolConfigDTO.builder()
                .id(null)
                .toolType(ToolType.CUSTOM)
                .name(TOOL_NAME)
                .description("获取知识库文件中指定行号范围内的文本块，按行号合并为纯文本返回，不传 endLine 时默认为文件最大行数")
                .parameterSchema("""
                        {
                          "type": "object",
                          "properties": {
                            "knowledgeBaseId": { "type": "string", "description": "知识库 ID" },
                            "fileId": { "type": "string", "description": "文件 ID" },
                            "startLine": { "type": "integer", "description": "起始行号，默认 0" },
                            "endLine": { "type": "integer", "description": "结束行号，默认文件最大行数" }
                          },
                          "required": ["knowledgeBaseId", "fileId"]
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
            JsonNode fileIdNode = root.get("fileId");
            if (fileIdNode == null || fileIdNode.isNull() || fileIdNode.asText().isBlank()) {
                return "{\"status\":\"error\",\"errMsg\":\"缺少 fileId 参数\"}";
            }
            String knowledgeBaseId = kbIdNode.asText();
            String fileId = fileIdNode.asText();
            int startLine = root.has("startLine") && root.get("startLine").canConvertToInt()
                    ? root.get("startLine").asInt() : DEFAULT_START_LINE;
            int endLine = root.has("endLine") && root.get("endLine").canConvertToInt()
                    ? root.get("endLine").asInt() : resolveFileMaxLine(knowledgeBaseId, fileId);

            TextChunkWithFile result = provider.getFileChunks(knowledgeBaseId, fileId, startLine, endLine);
            return mergeChunksToText(result);
        } catch (Exception e) {
            log.error("default_tool_rag_file_chunk 执行失败", e);
            return buildError(e.getMessage());
        }
    }

    /**
     * 将 TextChunkWithFile 中的文本块按行号升序合并为纯文本，块之间以换行分隔。
     */
    private static String mergeChunksToText(TextChunkWithFile withFile) {
        if (withFile == null || withFile.chunkList() == null || withFile.chunkList().isEmpty()) {
            return "";
        }
        return withFile.chunkList().stream()
                .sorted(Comparator.comparingInt(TextChunk::lineNumber))
                .map(TextChunk::text)
                .collect(Collectors.joining("\n"));
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

    private int resolveFileMaxLine(String knowledgeBaseId, String fileId) {
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
