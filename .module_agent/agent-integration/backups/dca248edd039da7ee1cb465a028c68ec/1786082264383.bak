package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class KnowledgeBaseInfoTool extends CustomToolInvoker {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    public static final String TOOL_NAME = "default_tool_rag_info";

    private final KnowledgeBaseQueryProvider provider;

    public KnowledgeBaseInfoTool(ToolConfigDTO toolConfig, KnowledgeBaseQueryProvider provider) {
        super(toolConfig);
        this.provider = provider;
    }

    public static ToolConfigDTO createToolConfig() {
        return ToolConfigDTO.builder()
                .id(null)
                .toolType(ToolType.CUSTOM)
                .name(TOOL_NAME)
                .description("获取当前会话关联的知识库信息")
                .parameterSchema("""
                        {
                          "type": "object",
                          "properties": {}
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
            KnowledgeBaseInfo info = provider.getKnowledgeBaseInfo(sessionId);
            return JSON_MAPPER.writeValueAsString(info);
        } catch (Exception e) {
            log.error("default_tool_rag_info 执行失败", e);
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
