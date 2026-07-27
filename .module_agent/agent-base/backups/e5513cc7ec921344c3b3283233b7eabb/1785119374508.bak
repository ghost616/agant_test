package com.ghost616.agentbase.service.agent.invoker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class UnloadSkillsSystemTool implements SystemTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String SESSION_KEY = "_sys_loading_SKILLS";

    @Override
    public String getToolName() {
        return "unload_skills";
    }

    @Override
    public String getDescription() {
        return "卸载指定的技能（SKILL），移除其工具";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"names\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"description\":\"要卸载的技能名称列表\"}},\"required\":[\"names\"]}";
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = MAPPER.readTree(arguments);
            JsonNode namesNode = root.get("names");
            if (namesNode == null || !namesNode.isArray()) {
                return "{\"error\":\"缺少 names 参数\"}";
            }

            Set<String> namesToRemove = new HashSet<>();
            for (JsonNode nameNode : namesNode) {
                if (nameNode != null && !nameNode.asText().isBlank()) {
                    namesToRemove.add(nameNode.asText().trim());
                }
            }

            List<String> loadedNames = readLoadedSkills(ctx);
            List<String> remaining = new ArrayList<>();
            int removedCount = 0;
            for (String name : loadedNames) {
                if (namesToRemove.contains(name)) {
                    removedCount++;
                } else {
                    remaining.add(name);
                }
            }

            ctx.putSessionVariable(SESSION_KEY, MAPPER.writeValueAsString(remaining));

            return "{\"status\":\"ok\",\"unloaded\":" + removedCount + "}";
        } catch (Exception e) {
            log.error("unload_skills 执行失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private List<String> readLoadedSkills(AgentExecutionContext ctx) {
        String json = ctx.getSessionVariable(SESSION_KEY);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
