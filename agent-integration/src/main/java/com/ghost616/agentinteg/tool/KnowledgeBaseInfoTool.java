package com.ghost616.agentinteg.tool;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseInfo;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseInfoTool implements SystemTool {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final String TOOL_NAME = "kb_info";

    private final KnowledgeBaseQueryProvider provider;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "获取当前会话关联的知识库信息";
    }

    @Override
    public String getParameterSchema() {
        return """
                {
                  "type": "object",
                  "properties": {}
                }""";
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
            log.error("kb_info 执行失败", e);
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
