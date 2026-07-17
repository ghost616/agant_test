package com.ghost616.agentbase.service.agent.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ghost616.agentbase.dto.model.ToolCall;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;

public final class ContextSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ContextSerializer() {
    }

    public static String serializeToJson(AgentExecutionContext ctx, String arguments) {
        try {
            ObjectNode root = MAPPER.createObjectNode();

            ObjectNode contextNode = MAPPER.createObjectNode();
            contextNode.put("sessionId", ctx.getSessionId() != null ? ctx.getSessionId().toString() : null);
            contextNode.put("agentId", ctx.getAgentId() != null ? ctx.getAgentId().toString() : null);
            contextNode.put("systemPrompt", ctx.getSystemPrompt());
            contextNode.put("modelId", ctx.getModelId() != null ? ctx.getModelId().toString() : null);
            contextNode.put("recentMessageCount", ctx.getRecentMessageCount());
            contextNode.put("stopped", ctx.isStopped());
            contextNode.put("projectDir", ctx.getProjectDir());

            ArrayNode historyArray = MAPPER.createArrayNode();
            for (AgentExecutionContext.HistoryEntry entry : ctx.getHistory()) {
                ObjectNode entryNode = MAPPER.createObjectNode();
                entryNode.put("role", entry.role());
                entryNode.put("content", entry.content());
                entryNode.put("reasoning", entry.reasoning());
                entryNode.put("toolCallId", entry.toolCallId());
                entryNode.put("sequenceNum", entry.sequenceNum());
                entryNode.put("createTime", entry.createTime() != null ? entry.createTime().toString() : null);
                if (entry.toolCalls() != null) {
                    ArrayNode toolCallsArray = MAPPER.createArrayNode();
                    for (ToolCall tc : entry.toolCalls()) {
                        ObjectNode tcNode = MAPPER.createObjectNode();
                        tcNode.put("id", tc.getId());
                        tcNode.put("name", tc.getName());
                        tcNode.put("arguments", tc.getArguments());
                        toolCallsArray.add(tcNode);
                    }
                    entryNode.set("toolCalls", toolCallsArray);
                }
                historyArray.add(entryNode);
            }
            contextNode.set("history", historyArray);

            ArrayNode toolsArray = MAPPER.createArrayNode();
            for (ToolConfigDTO tool : ctx.getTools()) {
                ObjectNode toolNode = MAPPER.createObjectNode();
                toolNode.put("name", tool.getName());
                toolNode.put("description", tool.getDescription());
                toolNode.put("parameterSchema", tool.getParameterSchema());
                toolsArray.add(toolNode);
            }
            contextNode.set("tools", toolsArray);

            ObjectNode sessionVarsNode = MAPPER.createObjectNode();
            for (String key : ctx.getSessionVariableKeys()) {
                String value = ctx.getSessionVariable(key);
                sessionVarsNode.put(key, value);
            }
            contextNode.set("sessionVariables", sessionVarsNode);

            ObjectNode conversationVarsNode = MAPPER.createObjectNode();
            for (String key : ctx.getConversationVariableKeys()) {
                String value = ctx.getConversationVariable(key);
                conversationVarsNode.put(key, value);
            }
            contextNode.set("conversationVariables", conversationVarsNode);

            ArrayNode skillsArray = MAPPER.createArrayNode();
            for (SkillConfigDTO skill : ctx.getSkills()) {
                ObjectNode skillNode = MAPPER.createObjectNode();
                skillNode.put("name", skill.getName());
                skillNode.put("description", skill.getDescription());
                skillNode.put("prompt", skill.getPrompt());
                if (skill.getSkillTools() != null) {
                    ArrayNode skillToolsArray = MAPPER.createArrayNode();
                    for (ToolConfigDTO st : skill.getSkillTools()) {
                        ObjectNode stNode = MAPPER.createObjectNode();
                        stNode.put("name", st.getName());
                        stNode.put("description", st.getDescription());
                        stNode.put("parameterSchema", st.getParameterSchema());
                        skillToolsArray.add(stNode);
                    }
                    skillNode.set("skillTools", skillToolsArray);
                }
                skillsArray.add(skillNode);
            }
            contextNode.set("skills", skillsArray);

            root.set("context", contextNode);
            root.put("arguments", arguments);

            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                    "序列化上下文失败: " + e.getMessage());
        }
    }
}
