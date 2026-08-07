package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.RequestType;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.agentinteg.model.PlatformType;
import com.ghost616.platform.entity.AgentSkill;
import com.ghost616.platform.entity.ModelConfig;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.entity.SessionSkill;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.entity.SkillTool;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.SessionSkillMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import com.ghost616.platform.repository.SkillToolMapper;
import com.ghost616.platform.service.tool.ToolConfigService;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SessionToolInfo;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SkillToolInfo;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentinteg.knowledge.KnowledgeBaseQueryProvider;
import com.ghost616.agentinteg.tool.BrowserToolCallback;
import com.ghost616.agentinteg.tool.BrowserToolInvoker;
import com.ghost616.agentinteg.tool.KnowledgeBaseInfoTool;
import com.ghost616.agentinteg.tool.KnowledgeFileChunkTool;
import com.ghost616.agentinteg.tool.KnowledgeFileInfoTool;
import com.ghost616.agentinteg.tool.KnowledgeSearchTool;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultToolDataProvider implements ToolDataProvider {

    private final SessionToolMapper sessionToolMapper;
    private final SessionMapper sessionMapper;
    private final AgentSkillMapper agentSkillMapper;
    private final SkillToolMapper skillToolMapper;
    private final SessionSkillMapper sessionSkillMapper;
    private final ToolConfigService toolConfigService;
    private final BrowserToolCallback browserToolCallback;
    private final ModelConfigMapper modelConfigMapper;
    private final ObjectProvider<KnowledgeBaseQueryProvider> knowledgeBaseQueryProvider;

    @Override
    public List<SessionToolInfo> getSessionToolIds(String sessionId) {
        Long sid = IdConverter.parse(sessionId);
        LambdaQueryWrapper<SessionTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionTool::getSessionId, sid);
        List<SessionTool> sessionTools = sessionToolMapper.selectList(wrapper);
        return sessionTools.stream()
                .map(st -> {
                    SessionAuthType auth = st.getSessionAuth();
                    return new SessionToolInfo(IdConverter.toString(st.getToolId()), auth != null ? auth : SessionAuthType.ALL);
                })
                .toList();
    }

    @Override
    public ToolConfigDTO getToolById(String toolId) {
        Long tid = IdConverter.parse(toolId);
        return toolConfigService.getById(tid);
    }

    @Override
    public CustomToolInvoker getCustomInvoker(ToolConfigDTO toolConfig) {
        ToolDetailDTO detail = toolConfigService.getById(IdConverter.parse(toolConfig.getId()));
        if (toolConfig.getToolType() == ToolType.CUSTOM && detail.getSubToolType() == SubToolType.BROWSER) {
            return new BrowserToolInvoker(toolConfig, browserToolCallback);
        }
        if (toolConfig.getToolType() == ToolType.CUSTOM && detail.getSubToolType() == SubToolType.RAG_KNOWLEDGE) {
            return createKnowledgeTool(toolConfig);
        }
        throw new UnsupportedOperationException("Custom tool invoker not supported");
    }

    private CustomToolInvoker createKnowledgeTool(ToolConfigDTO toolConfig) {
        KnowledgeBaseQueryProvider provider = knowledgeBaseQueryProvider.getObject();
        String name = toolConfig.getName();
        if (KnowledgeBaseInfoTool.TOOL_NAME.equals(name)) {
            return new KnowledgeBaseInfoTool(toolConfig, provider);
        }
        if (KnowledgeFileInfoTool.TOOL_NAME.equals(name)) {
            return new KnowledgeFileInfoTool(toolConfig, provider);
        }
        if (KnowledgeSearchTool.TOOL_NAME.equals(name)) {
            return new KnowledgeSearchTool(toolConfig, provider);
        }
        if (KnowledgeFileChunkTool.TOOL_NAME.equals(name)) {
            return new KnowledgeFileChunkTool(toolConfig, provider);
        }
        throw new UnsupportedOperationException("Unsupported RAG_KNOWLEDGE tool: " + name);
    }

    @Override
    public List<SkillToolInfo> getSkillToolIds(String sessionId) {
        Long sid = IdConverter.parse(sessionId);
        Session session = sessionMapper.selectById(sid);
        if (session == null) {
            return List.of();
        }

        if (Boolean.TRUE.equals(session.getIsChild())) {
            List<SessionSkill> sessionSkills = sessionSkillMapper.selectList(
                    new LambdaQueryWrapper<SessionSkill>()
                            .eq(SessionSkill::getSessionId, sid));
            if (sessionSkills == null || sessionSkills.isEmpty()) {
                return List.of();
            }
            List<Long> skillIds = sessionSkills.stream()
                    .map(SessionSkill::getSkillId)
                    .distinct()
                    .toList();
            List<SkillTool> skillTools = skillToolMapper.selectList(
                    new LambdaQueryWrapper<SkillTool>()
                            .in(SkillTool::getSkillId, skillIds));
            Map<Long, List<SkillTool>> groupedBySkill = skillTools.stream()
                    .collect(Collectors.groupingBy(SkillTool::getSkillId));
            return sessionSkills.stream()
                    .map(ss -> {
                        List<String> toolIds = groupedBySkill.getOrDefault(ss.getSkillId(), List.of())
                                .stream()
                                .map(st -> IdConverter.toString(st.getToolId()))
                                .distinct()
                                .toList();
                        return new SkillToolInfo(IdConverter.toString(ss.getSkillId()), ss.getSessionAuth(), toolIds);
                    })
                    .toList();
        }

        if (session.getAgentId() == null) {
            return List.of();
        }

        List<AgentSkill> agentSkills = agentSkillMapper.selectList(
                new LambdaQueryWrapper<AgentSkill>()
                        .eq(AgentSkill::getAgentId, session.getAgentId()));
        if (agentSkills == null || agentSkills.isEmpty()) {
            return List.of();
        }

        List<Long> skillIds = agentSkills.stream()
                .map(AgentSkill::getSkillId)
                .distinct()
                .toList();

        List<SkillTool> skillTools = skillToolMapper.selectList(
                new LambdaQueryWrapper<SkillTool>()
                        .in(SkillTool::getSkillId, skillIds));
        Map<Long, List<SkillTool>> groupedBySkill = skillTools.stream()
                .collect(Collectors.groupingBy(SkillTool::getSkillId));
        return agentSkills.stream()
                .map(as -> {
                    List<String> toolIds = groupedBySkill.getOrDefault(as.getSkillId(), List.of())
                            .stream()
                            .map(st -> IdConverter.toString(st.getToolId()))
                            .distinct()
                            .toList();
                    return new SkillToolInfo(IdConverter.toString(as.getSkillId()), as.getSessionAuth(), toolIds);
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> getBuiltinTools(String modelId) {
        Long mid = IdConverter.parse(modelId);
        if (mid == null) {
            return List.of();
        }
        ModelConfig modelConfig = modelConfigMapper.selectById(mid);
        if (modelConfig == null) {
            return List.of();
        }
        PlatformType platformType = modelConfig.getPlatformType();
        String requestType = modelConfig.getRequestType();
        boolean isOpenaiOrDeepseek = platformType == PlatformType.OPENAI || platformType == PlatformType.DEEPSEEK;
        if (isOpenaiOrDeepseek && RequestType.isResponses(requestType)) {
            return List.of(Map.of("type", "web_search"));
        }
        boolean isKimiDefault = requestType == null || requestType.isEmpty()
                || RequestType.COMPLETIONS.getCode().equals(requestType);
        if (platformType == PlatformType.KIMI && isKimiDefault) {
            return List.of(Map.of(
                    "type", "builtin_function",
                    "function", Map.of("name", "$web_search")));
        }
        return List.of();
    }

}