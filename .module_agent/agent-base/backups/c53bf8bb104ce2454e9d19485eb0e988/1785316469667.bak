package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.platform.entity.AgentSkill;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.entity.SessionSkill;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.entity.SkillTool;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.SessionSkillMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import com.ghost616.platform.repository.SkillToolMapper;
import com.ghost616.platform.service.tool.ToolConfigService;
import com.ghost616.agentbase.enums.ToolType;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider.SessionToolInfo;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvoker;
import com.ghost616.agentinteg.tool.BrowserToolCallback;
import com.ghost616.agentinteg.tool.BrowserToolInvoker;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.enums.SubToolType;
import com.ghost616.platform.util.IdConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        throw new UnsupportedOperationException("Custom tool invoker not supported");
    }

    @Override
    public List<String> getSkillToolIds(String sessionId) {
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
            return skillTools.stream()
                    .map(SkillTool::getToolId)
                    .map(IdConverter::toString)
                    .distinct()
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
        return skillTools.stream()
                .map(SkillTool::getToolId)
                .map(IdConverter::toString)
                .distinct()
                .toList();
    }

}