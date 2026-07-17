package com.ghost616.platform.systemtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.model.Message;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.ToolInvoker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class SystemTestSubSessionTool implements ToolInvoker {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(arguments);
            String sessionName = root.get("sessionName").asText();
            String message = root.get("message").asText();
            String description = root.has("description") && !root.get("description").isNull()
                    ? root.get("description").asText() : "system test sub-session";

            Long modelId = ctx.getModelId();
            List<Long> toolIds = ctx.getTools() != null && !ctx.getTools().isEmpty()
                    ? ctx.getTools().stream().map(ToolConfigDTO::getId).distinct().toList() : null;
            List<Long> skillIds = ctx.getSkills() != null && !ctx.getSkills().isEmpty()
                    ? ctx.getSkills().stream().map(SkillConfigDTO::getId).toList() : null;

            Long childSessionId = ctx.createChildSession(sessionName, description, modelId, toolIds, skillIds, null);
            if (childSessionId == null) {
                return "{\"error\":\"createChildSession returned null\"}";
            }

            Boolean thinking = root.has("thinking") && !root.get("thinking").isNull()
                    ? root.get("thinking").asBoolean() : null;
            Message reply = ctx.sendUserMessage(childSessionId, message, modelId, thinking);

            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "role", reply.getRole(),
                    "content", reply.getContent()
            ));
        } catch (Exception e) {
            log.error("system_test 执行失败", e);
            try {
                return OBJECT_MAPPER.writeValueAsString(Map.of("error", e.getMessage()));
            } catch (Exception inner) {
                return "{\"error\":\"" + inner.getMessage() + "\"}";
            }
        }
    }
}
