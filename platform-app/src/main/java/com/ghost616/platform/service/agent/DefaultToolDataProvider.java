package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.platform.entity.AgentSkill;
import com.ghost616.platform.entity.Session;
import com.ghost616.platform.entity.SessionTool;
import com.ghost616.platform.entity.SkillTool;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.repository.SessionToolMapper;
import com.ghost616.platform.repository.SkillToolMapper;
import com.ghost616.platform.service.tool.ToolConfigService;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
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
    private final ToolConfigService toolConfigService;

    @Override
    public List<Long> getSessionToolIds(Long sessionId) {
        LambdaQueryWrapper<SessionTool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionTool::getSessionId, sessionId);
        List<SessionTool> sessionTools = sessionToolMapper.selectList(wrapper);
        return sessionTools.stream()
                .map(SessionTool::getToolId)
                .toList();
    }

    @Override
    public ToolConfigDTO getToolById(Long toolId) {
        return toolConfigService.getById(toolId);
    }

    @Override
    public List<Long> getSkillToolIds(Long sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || session.getAgentId() == null) {
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
                .distinct()
                .toList();
    }

}