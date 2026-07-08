package com.ghost616.agentbase.service.agent.invoker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class LoadSkillsSystemTool implements SystemTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SESSION_KEY = "_sys_loading_SKILLS";

    @Override
    public String getToolName() {
        return "load_skills";
    }

    @Override
    public String getDescription() {
        return "加载指定的技能（SKILL），使其工具在对话中可用";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"names\":{\"type\":\"array\",\"items\":{\"type\":\"string\"},\"description\":\"要加载的技能名称列表\"}},\"required\":[\"names\"]}";
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            JsonNode root = MAPPER.readTree(arguments);
            JsonNode namesNode = root.get("names");
            if (namesNode == null || !namesNode.isArray()) {
                return "{\"error\":\"缺少 names 参数\"}";
            }

            List<String> requestedNames = new ArrayList<>();
            for (JsonNode nameNode : namesNode) {
                if (nameNode != null && !nameNode.asText().isBlank()) {
                    requestedNames.add(nameNode.asText().trim());
                }
            }

            List<SkillConfigDTO> availableSkills = ctx.getSkills();
            Set<String> availableNames = new HashSet<>();
            for (SkillConfigDTO skill : availableSkills) {
                availableNames.add(skill.getName());
            }

            List<String> loadedNames = readLoadedSkills(ctx);
            Set<String> loadedSet = new HashSet<>(loadedNames);

            List<String> newlyLoaded = new ArrayList<>();
            for (String name : requestedNames) {
                if (!availableNames.contains(name)) {
                    log.debug("技能 '{}' 不存在于智能体配置中，跳过", name);
                    continue;
                }
                if (!loadedSet.contains(name)) {
                    loadedSet.add(name);
                    newlyLoaded.add(name);
                }
            }

            ctx.putSessionVariable(SESSION_KEY, MAPPER.writeValueAsString(new ArrayList<>(loadedSet)));

            return "{\"status\":\"ok\",\"loaded\":\"" + newlyLoaded.size() + "\",\"names\":" + MAPPER.writeValueAsString(newlyLoaded) + "}";
        } catch (Exception e) {
            log.error("load_skills 执行失败", e);
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
