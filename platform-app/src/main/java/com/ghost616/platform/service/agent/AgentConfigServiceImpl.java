package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.agentbase.enums.SessionAuthType;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.dto.agent.AgentConfigDTO;
import com.ghost616.platform.dto.agent.AgentCreateRequest;
import com.ghost616.platform.dto.agent.AgentSkillItem;
import com.ghost616.platform.dto.agent.AgentToolItem;
import com.ghost616.platform.dto.agent.AgentUpdateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseDTO;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.AgentKnowledgeBase;
import com.ghost616.platform.entity.AgentSkill;
import com.ghost616.platform.entity.AgentTool;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.AgentSkillMapper;
import com.ghost616.platform.repository.AgentToolMapper;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.SkillConfigMapper;
import com.ghost616.platform.session.UserContext;
import com.ghost616.platform.session.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AgentConfigServiceImpl implements AgentConfigService {

    private final AgentConfigMapper agentConfigMapper;
    private final AgentToolMapper agentToolMapper;
    private final AgentSkillMapper agentSkillMapper;
    private final SkillConfigMapper skillConfigMapper;
    private final AgentKnowledgeBaseService agentKnowledgeBaseService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public List<AgentConfigDTO> list(String name, CommonStatus status) {
        Long userId = currentUserId();
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConfig::getUserId, userId);
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(AgentConfig::getName, name);
        }
        if (status != null) {
            wrapper.eq(AgentConfig::getStatus, status);
        }
        wrapper.orderByDesc(AgentConfig::getCreateTime);

        List<AgentConfig> entities = agentConfigMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public AgentConfigDTO getById(Long id) {
        AgentConfig entity = agentConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    @Transactional
    public AgentConfigDTO create(AgentCreateRequest request) {
        Long userId = currentUserId();
        checkNameDuplicate(request.getName(), null);

        validateMemoryConfig(request.getMemoryEnabled(), request.getMemoryGroupCount(),
                request.getRecentMessageCount(), request.getVectorModelId());

        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            validateSkillIds(request.getSkills());
        }

        AgentConfig entity = new AgentConfig();
        entity.setUserId(userId);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setModelId(request.getModelId());
        entity.setRecentMessageCount(request.getRecentMessageCount());
        entity.setMemoryEnabled(request.getMemoryEnabled() != null ? request.getMemoryEnabled() : false);
        entity.setMemoryGroupCount(request.getMemoryGroupCount() != null ? request.getMemoryGroupCount() : 30);
        entity.setVectorModelId(request.getVectorModelId());
        entity.setStatus(CommonStatus.ENABLED);

        agentConfigMapper.insert(entity);

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            for (AgentToolItem item : request.getTools()) {
                AgentTool agentTool = new AgentTool();
                agentTool.setAgentId(entity.getId());
                agentTool.setToolId(item.toolId());
                agentTool.setSessionAuth(Optional.ofNullable(item.sessionAuth()).orElse(SessionAuthType.ALL));
                agentToolMapper.insert(agentTool);
            }
        }

        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            for (AgentSkillItem item : request.getSkills()) {
                AgentSkill agentSkill = new AgentSkill();
                agentSkill.setAgentId(entity.getId());
                agentSkill.setSkillId(item.skillId());
                agentSkill.setSessionAuth(Optional.ofNullable(item.sessionAuth()).orElse(SessionAuthType.ALL));
                agentSkillMapper.insert(agentSkill);
            }
        }

        if (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty()) {
            List<AgentKnowledgeBase> agentKnowledgeBases = request.getKnowledgeBaseIds().stream()
                    .map(knowledgeBaseId -> {
                        AgentKnowledgeBase agentKnowledgeBase = new AgentKnowledgeBase();
                        agentKnowledgeBase.setAgentId(entity.getId());
                        agentKnowledgeBase.setKnowledgeBaseId(knowledgeBaseId);
                        return agentKnowledgeBase;
                    })
                    .toList();
            agentKnowledgeBaseService.saveBatch(agentKnowledgeBases);
        }

        return toDTO(entity);
    }

    @Override
    @Transactional
    public AgentConfigDTO update(Long id, AgentUpdateRequest request) {
        AgentConfig entity = agentConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        if (StringUtils.isNotBlank(request.getName())) {
            checkNameDuplicate(request.getName(), id);
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getSystemPrompt() != null) {
            entity.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getModelId() != null) {
            entity.setModelId(request.getModelId());
        }
        if (request.getRecentMessageCount() != null) {
            entity.setRecentMessageCount(request.getRecentMessageCount());
        }
        if (request.getMemoryEnabled() != null) {
            entity.setMemoryEnabled(request.getMemoryEnabled());
        }
        if (request.getMemoryGroupCount() != null) {
            entity.setMemoryGroupCount(request.getMemoryGroupCount());
        }
        if (request.getVectorModelId() != null) {
            entity.setVectorModelId(request.getVectorModelId());
        }

        validateMemoryConfig(
                request.getMemoryEnabled() != null ? request.getMemoryEnabled() : entity.getMemoryEnabled(),
                request.getMemoryGroupCount() != null ? request.getMemoryGroupCount() : entity.getMemoryGroupCount(),
                request.getRecentMessageCount() != null ? request.getRecentMessageCount() : entity.getRecentMessageCount(),
                request.getVectorModelId() != null ? request.getVectorModelId() : entity.getVectorModelId());

        agentConfigMapper.updateById(entity);

        if (request.getTools() != null) {
            LambdaQueryWrapper<AgentTool> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(AgentTool::getAgentId, id);
            agentToolMapper.delete(deleteWrapper);

            if (!request.getTools().isEmpty()) {
                for (AgentToolItem item : request.getTools()) {
                    AgentTool agentTool = new AgentTool();
                    agentTool.setAgentId(id);
                    agentTool.setToolId(item.toolId());
                    agentTool.setSessionAuth(Optional.ofNullable(item.sessionAuth()).orElse(SessionAuthType.ALL));
                    agentToolMapper.insert(agentTool);
                }
            }
        }

        if (request.getSkills() != null) {
            validateSkillIds(request.getSkills());

            LambdaQueryWrapper<AgentSkill> skillDeleteWrapper = new LambdaQueryWrapper<>();
            skillDeleteWrapper.eq(AgentSkill::getAgentId, id);
            agentSkillMapper.delete(skillDeleteWrapper);

            if (!request.getSkills().isEmpty()) {
                for (AgentSkillItem item : request.getSkills()) {
                    AgentSkill agentSkill = new AgentSkill();
                    agentSkill.setAgentId(id);
                    agentSkill.setSkillId(item.skillId());
                    agentSkill.setSessionAuth(Optional.ofNullable(item.sessionAuth()).orElse(SessionAuthType.ALL));
                    agentSkillMapper.insert(agentSkill);
                }
            }
        }

        if (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty()) {
            LambdaQueryWrapper<AgentKnowledgeBase> kbDeleteWrapper = new LambdaQueryWrapper<>();
            kbDeleteWrapper.eq(AgentKnowledgeBase::getAgentId, id);
            agentKnowledgeBaseService.remove(kbDeleteWrapper);

            List<AgentKnowledgeBase> agentKnowledgeBases = request.getKnowledgeBaseIds().stream()
                    .map(knowledgeBaseId -> {
                        AgentKnowledgeBase agentKnowledgeBase = new AgentKnowledgeBase();
                        agentKnowledgeBase.setAgentId(id);
                        agentKnowledgeBase.setKnowledgeBaseId(knowledgeBaseId);
                        return agentKnowledgeBase;
                    })
                    .toList();
            agentKnowledgeBaseService.saveBatch(agentKnowledgeBases);
        }

        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AgentConfig entity = agentConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        LambdaQueryWrapper<AgentTool> toolDeleteWrapper = new LambdaQueryWrapper<>();
        toolDeleteWrapper.eq(AgentTool::getAgentId, id);
        agentToolMapper.delete(toolDeleteWrapper);

        LambdaQueryWrapper<AgentSkill> skillDeleteWrapper = new LambdaQueryWrapper<>();
        skillDeleteWrapper.eq(AgentSkill::getAgentId, id);
        agentSkillMapper.delete(skillDeleteWrapper);

        LambdaQueryWrapper<AgentKnowledgeBase> kbDeleteWrapper = new LambdaQueryWrapper<>();
        kbDeleteWrapper.eq(AgentKnowledgeBase::getAgentId, id);
        agentKnowledgeBaseService.remove(kbDeleteWrapper);

        agentConfigMapper.deleteById(id);
    }

    @Override
    public AgentConfigDTO toggleStatus(Long id, CommonStatus status) {
        AgentConfig entity = agentConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }
        entity.setStatus(status);
        agentConfigMapper.updateById(entity);
        return toDTO(entity);
    }

    private void validateMemoryConfig(Boolean memoryEnabled, Integer memoryGroupCount, Integer recentMessageCount,
                                      Long vectorModelId) {
        if (!Boolean.TRUE.equals(memoryEnabled)) {
            return;
        }
        if (vectorModelId == null) {
            throw new BusinessException(ErrorCode.AGENT_MEMORY_VECTOR_MODEL_REQUIRED);
        }
        int effectiveGroupCount = memoryGroupCount != null ? memoryGroupCount : 30;
        int effectiveRecentCount = recentMessageCount != null ? recentMessageCount : 10;
        if (effectiveGroupCount < effectiveRecentCount * 3) {
            throw new BusinessException(ErrorCode.AGENT_MEMORY_GROUP_INVALID);
        }
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConfig::getUserId, currentUserId());
        wrapper.eq(AgentConfig::getName, name);
        if (excludeId != null) {
            wrapper.ne(AgentConfig::getId, excludeId);
        }
        if (agentConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_EXISTS);
        }
    }

    /**
     * 获取当前登录用户 ID。
     *
     * <p>从 {@link UserContext} 线程上下文读取用户会话；
     * 未登录时抛出 {@link ErrorCode#USER_NOT_LOGIN}，防止无归属数据写入。</p>
     *
     * @return 当前登录用户 ID
     */
    private Long currentUserId() {
        UserSession session = UserContext.get();
        if (session == null || session.getUser() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_LOGIN);
        }
        return session.getUser().getId();
    }

    private AgentConfigDTO toDTO(AgentConfig entity) {
        LambdaQueryWrapper<AgentTool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(AgentTool::getAgentId, entity.getId());
        List<AgentTool> agentTools = agentToolMapper.selectList(toolWrapper);
        List<AgentToolItem> tools = agentTools.stream()
                .map(t -> new AgentToolItem(t.getToolId(), t.getSessionAuth()))
                .toList();

        LambdaQueryWrapper<AgentSkill> skillWrapper = new LambdaQueryWrapper<>();
        skillWrapper.eq(AgentSkill::getAgentId, entity.getId());
        List<AgentSkill> agentSkills = agentSkillMapper.selectList(skillWrapper);
        List<AgentSkillItem> skills = agentSkills.stream()
                .map(s -> new AgentSkillItem(s.getSkillId(), s.getSessionAuth()))
                .toList();

        List<KnowledgeBaseDTO> knowledgeBases = loadKnowledgeBases(entity.getId());

        return AgentConfigDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .systemPrompt(entity.getSystemPrompt())
                .modelId(entity.getModelId())
                .status(entity.getStatus())
                .recentMessageCount(entity.getRecentMessageCount())
                .memoryEnabled(entity.getMemoryEnabled())
                .memoryGroupCount(entity.getMemoryGroupCount())
                .vectorModelId(entity.getVectorModelId())
                .tools(tools)
                .skills(skills)
                .knowledgeBases(knowledgeBases)
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private List<KnowledgeBaseDTO> loadKnowledgeBases(Long agentId) {
        LambdaQueryWrapper<AgentKnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
        kbWrapper.eq(AgentKnowledgeBase::getAgentId, agentId);
        List<AgentKnowledgeBase> agentKnowledgeBases = agentKnowledgeBaseService.list(kbWrapper);
        if (agentKnowledgeBases.isEmpty()) {
            return List.of();
        }

        List<Long> knowledgeBaseIds = agentKnowledgeBases.stream()
                .map(AgentKnowledgeBase::getKnowledgeBaseId)
                .toList();
        Map<Long, KnowledgeBase> kbMap = knowledgeBaseMapper.selectBatchIds(knowledgeBaseIds).stream()
                .collect(Collectors.toMap(KnowledgeBase::getId, Function.identity()));

        return agentKnowledgeBases.stream()
                .map(akb -> {
                    KnowledgeBase kb = kbMap.get(akb.getKnowledgeBaseId());
                    return KnowledgeBaseDTO.builder()
                            .id(akb.getKnowledgeBaseId())
                            .name(kb != null ? kb.getName() : null)
                            .build();
                })
                .toList();
    }

    private void validateSkillIds(List<AgentSkillItem> skills) {
        if (skills == null || skills.isEmpty()) {
            return;
        }
        List<Long> skillIds = skills.stream().map(AgentSkillItem::skillId).toList();
        int count = skillConfigMapper.selectBatchIds(skillIds).size();
        if (count != skillIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "部分关联技能不存在");
        }
    }
}
