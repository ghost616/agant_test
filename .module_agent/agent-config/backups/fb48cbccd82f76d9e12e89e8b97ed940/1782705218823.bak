package com.ghost616.platform.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ghost616.platform.dto.agent.AgentConfigDTO;
import com.ghost616.platform.dto.agent.AgentCreateRequest;
import com.ghost616.platform.dto.agent.AgentUpdateRequest;
import com.ghost616.platform.entity.AgentConfig;
import com.ghost616.platform.entity.AgentTool;
import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.AgentConfigMapper;
import com.ghost616.platform.repository.AgentToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentConfigServiceImpl implements AgentConfigService {

    private final AgentConfigMapper agentConfigMapper;
    private final AgentToolMapper agentToolMapper;

    @Override
    public List<AgentConfigDTO> list(String name, CommonStatus status) {
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<>();
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
    public AgentConfigDTO create(AgentCreateRequest request) {
        checkNameDuplicate(request.getName(), null);

        AgentConfig entity = new AgentConfig();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSystemPrompt(request.getSystemPrompt());
        entity.setModelId(request.getModelId());
        entity.setRecentMessageCount(request.getRecentMessageCount());
        entity.setStatus(CommonStatus.ENABLED);

        agentConfigMapper.insert(entity);

        if (request.getToolIds() != null && !request.getToolIds().isEmpty()) {
            for (Long toolId : request.getToolIds()) {
                AgentTool agentTool = new AgentTool();
                agentTool.setAgentId(entity.getId());
                agentTool.setToolId(toolId);
                agentToolMapper.insert(agentTool);
            }
        }

        return toDTO(entity);
    }

    @Override
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

        agentConfigMapper.updateById(entity);

        if (request.getToolIds() != null) {
            LambdaQueryWrapper<AgentTool> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(AgentTool::getAgentId, id);
            agentToolMapper.delete(deleteWrapper);

            if (!request.getToolIds().isEmpty()) {
                for (Long toolId : request.getToolIds()) {
                    AgentTool agentTool = new AgentTool();
                    agentTool.setAgentId(id);
                    agentTool.setToolId(toolId);
                    agentToolMapper.insert(agentTool);
                }
            }
        }

        return toDTO(entity);
    }

    @Override
    public void delete(Long id) {
        AgentConfig entity = agentConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_FOUND);
        }

        LambdaQueryWrapper<AgentTool> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(AgentTool::getAgentId, id);
        agentToolMapper.delete(deleteWrapper);

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

    private void checkNameDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConfig::getName, name);
        if (excludeId != null) {
            wrapper.ne(AgentConfig::getId, excludeId);
        }
        if (agentConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.AGENT_ALREADY_EXISTS);
        }
    }

    private AgentConfigDTO toDTO(AgentConfig entity) {
        LambdaQueryWrapper<AgentTool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(AgentTool::getAgentId, entity.getId());
        List<AgentTool> agentTools = agentToolMapper.selectList(toolWrapper);
        List<Long> toolIds = agentTools.stream()
                .map(AgentTool::getToolId)
                .toList();

        return AgentConfigDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .systemPrompt(entity.getSystemPrompt())
                .modelId(entity.getModelId())
                .status(entity.getStatus())
                .recentMessageCount(entity.getRecentMessageCount())
                .toolIds(toolIds)
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
