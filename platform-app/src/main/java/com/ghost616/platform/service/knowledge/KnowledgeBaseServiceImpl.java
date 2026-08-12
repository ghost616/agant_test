package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseUpdateRequest;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String ES_INDEX_PREFIX = "agent_";
    private static final char[] ES_INDEX_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyz_".toCharArray();
    private static final int ES_INDEX_RANDOM_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;

    @Override
    public List<KnowledgeBaseDTO> list(String name, CommonStatus status) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(KnowledgeBase::getName, name);
        }
        if (status != null) {
            wrapper.eq(KnowledgeBase::getStatus, status);
        }
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);

        List<KnowledgeBase> entities = knowledgeBaseMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public KnowledgeBaseDTO getById(Long id) {
        KnowledgeBase entity = knowledgeBaseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    public KnowledgeBaseDTO create(KnowledgeBaseCreateRequest request) {
        checkNameDuplicate(request.getName(), null);

        KnowledgeBase entity = new KnowledgeBase();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setVectorModelId(request.getVectorModelId());
        if (StringUtils.isNotBlank(request.getEsIndex())) {
            entity.setEsIndex(request.getEsIndex());
        }
        entity.setStatus(CommonStatus.ENABLED);

        knowledgeBaseMapper.insert(entity);
        if (StringUtils.isBlank(entity.getEsIndex())) {
            entity.setEsIndex(generateEsIndex(entity.getId()));
            knowledgeBaseMapper.updateById(entity);
        }
        return toDTO(entity);
    }

    @Override
    public KnowledgeBaseDTO update(Long id, KnowledgeBaseUpdateRequest request) {
        KnowledgeBase entity = knowledgeBaseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        if (StringUtils.isNotBlank(request.getName())) {
            checkNameDuplicate(request.getName(), id);
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getVectorModelId() != null) {
            entity.setVectorModelId(request.getVectorModelId());
        }
        if (request.getEsIndex() != null) {
            entity.setEsIndex(request.getEsIndex());
        }
        if (StringUtils.isBlank(entity.getEsIndex())) {
            entity.setEsIndex(generateEsIndex(entity.getId()));
        }

        knowledgeBaseMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        KnowledgeBase entity = knowledgeBaseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        LambdaQueryWrapper<KnowledgeFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(KnowledgeFile::getKnowledgeBaseId, id);
        knowledgeFileMapper.delete(fileWrapper);

        knowledgeBaseMapper.deleteById(id);
    }

    @Override
    public KnowledgeBaseDTO toggleStatus(Long id, CommonStatus status) {
        KnowledgeBase entity = knowledgeBaseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        entity.setStatus(status);
        knowledgeBaseMapper.updateById(entity);
        return toDTO(entity);
    }

    private void checkNameDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getName, name);
        if (excludeId != null) {
            wrapper.ne(KnowledgeBase::getId, excludeId);
        }
        if (knowledgeBaseMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_ALREADY_EXISTS);
        }
    }

    private String generateEsIndex(Long knowledgeBaseId) {
        StringBuilder suffix = new StringBuilder(ES_INDEX_RANDOM_LENGTH);
        for (int i = 0; i < ES_INDEX_RANDOM_LENGTH; i++) {
            suffix.append(ES_INDEX_CHARS[random.nextInt(ES_INDEX_CHARS.length)]);
        }
        return ES_INDEX_PREFIX + knowledgeBaseId + "_" + suffix;
    }

    private KnowledgeBaseDTO toDTO(KnowledgeBase entity) {
        return KnowledgeBaseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .vectorModelId(entity.getVectorModelId())
                .esIndex(entity.getEsIndex())
                .rebuilding(entity.getRebuilding())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
