package com.ghost616.platform.service.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ghost616.platform.dto.skill.SkillConfigDTO;
import com.ghost616.platform.dto.skill.SkillCreateRequest;
import com.ghost616.platform.dto.skill.SkillUpdateRequest;
import com.ghost616.platform.entity.SkillConfig;
import com.ghost616.platform.entity.SkillTool;
import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.SkillConfigMapper;
import com.ghost616.platform.repository.SkillToolMapper;
import com.ghost616.platform.repository.ToolConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillConfigServiceImpl implements SkillConfigService {

    private final SkillConfigMapper skillConfigMapper;
    private final SkillToolMapper skillToolMapper;
    private final ToolConfigMapper toolConfigMapper;

    @Override
    public List<SkillConfigDTO> list(String name, CommonStatus status) {
        LambdaQueryWrapper<SkillConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(SkillConfig::getName, name);
        }
        if (status != null) {
            wrapper.eq(SkillConfig::getStatus, status);
        }
        wrapper.orderByDesc(SkillConfig::getCreateTime);

        List<SkillConfig> entities = skillConfigMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public SkillConfigDTO getById(Long id) {
        SkillConfig entity = skillConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SKILL_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    @Transactional
    public SkillConfigDTO create(SkillCreateRequest request) {
        checkNameDuplicate(request.getName(), null);

        SkillConfig entity = SkillConfig.builder()
                .name(request.getName())
                .description(request.getDescription())
                .prompt(request.getPrompt())
                .status(CommonStatus.ENABLED)
                .build();

        skillConfigMapper.insert(entity);

        if (request.getToolIds() != null && !request.getToolIds().isEmpty()) {
            validateToolIds(request.getToolIds());
            for (Long toolId : request.getToolIds()) {
                SkillTool skillTool = new SkillTool();
                skillTool.setSkillId(entity.getId());
                skillTool.setToolId(toolId);
                skillToolMapper.insert(skillTool);
            }
        }

        return toDTO(entity);
    }

    @Override
    @Transactional
    public SkillConfigDTO update(Long id, SkillUpdateRequest request) {
        SkillConfig entity = skillConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SKILL_NOT_FOUND);
        }

        if (StringUtils.isNotBlank(request.getName())) {
            checkNameDuplicate(request.getName(), id);
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getPrompt() != null) {
            entity.setPrompt(request.getPrompt());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        skillConfigMapper.updateById(entity);

        if (request.getToolIds() != null) {
            LambdaQueryWrapper<SkillTool> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SkillTool::getSkillId, id);
            skillToolMapper.delete(deleteWrapper);

            if (!request.getToolIds().isEmpty()) {
                validateToolIds(request.getToolIds());
                for (Long toolId : request.getToolIds()) {
                    SkillTool skillTool = new SkillTool();
                    skillTool.setSkillId(id);
                    skillTool.setToolId(toolId);
                    skillToolMapper.insert(skillTool);
                }
            }
        }

        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SkillConfig entity = skillConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SKILL_NOT_FOUND);
        }

        LambdaQueryWrapper<SkillTool> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SkillTool::getSkillId, id);
        skillToolMapper.delete(deleteWrapper);

        skillConfigMapper.deleteById(id);
    }

    @Override
    public SkillConfigDTO toggleStatus(Long id, CommonStatus status) {
        SkillConfig entity = skillConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.SKILL_NOT_FOUND);
        }
        entity.setStatus(status);
        skillConfigMapper.updateById(entity);
        return toDTO(entity);
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<SkillConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillConfig::getName, name);
        if (excludeId != null) {
            wrapper.ne(SkillConfig::getId, excludeId);
        }
        if (skillConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.SKILL_ALREADY_EXISTS);
        }
    }

    private void validateToolIds(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return;
        }
        int count = toolConfigMapper.selectBatchIds(toolIds).size();
        if (count != toolIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "部分关联工具不存在");
        }
    }

    private SkillConfigDTO toDTO(SkillConfig entity) {
        LambdaQueryWrapper<SkillTool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(SkillTool::getSkillId, entity.getId());
        List<SkillTool> skillTools = skillToolMapper.selectList(toolWrapper);
        List<Long> toolIds = skillTools.stream()
                .map(SkillTool::getToolId)
                .toList();

        return SkillConfigDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .prompt(entity.getPrompt())
                .status(entity.getStatus())
                .toolIds(toolIds)
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
