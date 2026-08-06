package com.ghost616.platform.service.knowledge;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.dto.knowledge.KnowledgeFileCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeFileDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeFileUpdateRequest;

import java.util.List;

public interface KnowledgeFileService {

    List<KnowledgeFileDTO> list(Long knowledgeBaseId, String fileName, CommonStatus status);

    KnowledgeFileDTO getById(Long id);

    String getFileContent(Long id);

    void updateFileContent(Long id, String content);

    KnowledgeFileDTO create(Long knowledgeBaseId, KnowledgeFileCreateRequest request);

    KnowledgeFileDTO update(Long id, KnowledgeFileUpdateRequest request);

    void delete(Long id);

    KnowledgeFileDTO toggleStatus(Long id, CommonStatus status);
}
