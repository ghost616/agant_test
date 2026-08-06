package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.dto.knowledge.KnowledgeFileCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeFileDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeFileUpdateRequest;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.enums.PublishStatus;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl implements KnowledgeFileService {

    private final KnowledgeFileMapper knowledgeFileMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgePublishService knowledgePublishService;

    @Override
    public List<KnowledgeFileDTO> list(Long knowledgeBaseId, String fileName, CommonStatus status) {
        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<>();
        if (knowledgeBaseId != null) {
            wrapper.eq(KnowledgeFile::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (StringUtils.isNotBlank(fileName)) {
            wrapper.like(KnowledgeFile::getFileName, fileName);
        }
        if (status != null) {
            wrapper.eq(KnowledgeFile::getStatus, status);
        }
        wrapper.orderByDesc(KnowledgeFile::getCreateTime);

        List<KnowledgeFile> entities = knowledgeFileMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).toList();
    }

    @Override
    public KnowledgeFileDTO getById(Long id) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
        return toDTO(entity);
    }

    @Override
    public KnowledgeFileDTO create(Long knowledgeBaseId, KnowledgeFileCreateRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        KnowledgeFile entity = new KnowledgeFile();
        entity.setFileName(request.getFileName());
        entity.setFileDescription(request.getFileDescription());
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setStatus(CommonStatus.ENABLED);
        entity.setPublishStatus(PublishStatus.UNPUBLISHED);

        knowledgeFileMapper.insert(entity);
        return toDTO(entity);
    }

    @Override
    public String getFileContent(Long id) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
        return entity.getFileContent();
    }

    @Override
    public void updateFileContent(Long id, String content) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
        if (knowledgePublishService.isPublishing(id)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_PUBLISHING);
        }
        entity.setFileContent(content);
        entity.setFileSize(computeFileSize(content));
        entity.setLineCount(computeLineCount(content));
        if (entity.getPublishStatus() == PublishStatus.PUBLISHED) {
            entity.setPublishStatus(PublishStatus.PENDING_PUBLISH);
        }
        knowledgeFileMapper.updateById(entity);
    }

    @Override
    public KnowledgeFileDTO update(Long id, KnowledgeFileUpdateRequest request) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }

        if (StringUtils.isNotBlank(request.getFileName())) {
            entity.setFileName(request.getFileName());
        }
        if (request.getFileDescription() != null) {
            entity.setFileDescription(request.getFileDescription());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        knowledgeFileMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public void delete(Long id) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
        knowledgeFileMapper.deleteById(id);
    }

    @Override
    public KnowledgeFileDTO toggleStatus(Long id, CommonStatus status) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
        entity.setStatus(status);
        knowledgeFileMapper.updateById(entity);
        return toDTO(entity);
    }

    private long computeFileSize(String content) {
        if (content == null) {
            return 0L;
        }
        return content.getBytes(StandardCharsets.UTF_8).length;
    }

    private int computeLineCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n", -1).length;
    }

    private KnowledgeFileDTO toDTO(KnowledgeFile entity) {
        return KnowledgeFileDTO.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .fileDescription(entity.getFileDescription())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .fileSize(entity.getFileSize())
                .lineCount(entity.getLineCount())
                .status(entity.getStatus())
                .publishStatus(entity.getPublishStatus())
                .publishing(knowledgePublishService.isPublishing(entity.getId()))
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
