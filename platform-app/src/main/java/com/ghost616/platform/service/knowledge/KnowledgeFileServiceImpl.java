package com.ghost616.platform.service.knowledge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.dto.knowledge.KnowledgeFileCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeFileDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeFileUpdateRequest;
import com.ghost616.platform.entity.KnowledgeBase;
import com.ghost616.platform.entity.KnowledgeFile;
import com.ghost616.platform.enums.PublishStatus;
import com.ghost616.platform.repository.KnowledgeBaseMapper;
import com.ghost616.platform.repository.KnowledgeFileMapper;
import com.ghost616.platform.session.UserContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识文件业务实现。创建数据时从 {@link UserContextUtil} 获取当前登录用户填充 user_id，
 * 查询/列表仅返回当前用户数据，单条访问校验数据归属，实现知识文件数据用户隔离。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl implements KnowledgeFileService {

    private final KnowledgeFileMapper knowledgeFileMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgePublishService knowledgePublishService;

    @Override
    public List<KnowledgeFileDTO> list(Long knowledgeBaseId, String fileName, CommonStatus status) {
        LambdaQueryWrapper<KnowledgeFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeFile::getUserId, UserContextUtil.requireUserId());
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
        requireOwned(entity);
        return toDTO(entity);
    }

    @Override
    public KnowledgeFileDTO create(Long knowledgeBaseId, KnowledgeFileCreateRequest request) {
        Long userId = UserContextUtil.requireUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        requireOwned(knowledgeBase);

        KnowledgeFile entity = new KnowledgeFile();
        entity.setUserId(userId);
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
        requireOwned(entity);
        return entity.getFileContent();
    }

    @Override
    public void updateFileContent(Long id, String content) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
        requireOwned(entity);
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
        requireOwned(entity);

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
        requireOwned(entity);
        knowledgeFileMapper.deleteById(id);
    }

    @Override
    public KnowledgeFileDTO toggleStatus(Long id, CommonStatus status) {
        KnowledgeFile entity = knowledgeFileMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
        requireOwned(entity);
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

    /**
     * 校验知识文件归属当前用户，非本人数据按不存在处理（不泄露数据存在性）。
     *
     * @param entity 知识文件实体
     */
    private void requireOwned(KnowledgeFile entity) {
        Long userId = UserContextUtil.requireUserId();
        if (entity.getUserId() != null && !entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_NOT_FOUND);
        }
    }

    /**
     * 校验知识库归属当前用户，非本人数据按不存在处理（不泄露数据存在性）。
     *
     * @param entity 知识库实体
     */
    private void requireOwned(KnowledgeBase entity) {
        Long userId = UserContextUtil.requireUserId();
        if (entity.getUserId() != null && !entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
    }
}
