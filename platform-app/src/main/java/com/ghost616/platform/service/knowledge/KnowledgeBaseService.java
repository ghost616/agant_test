package com.ghost616.platform.service.knowledge;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseUpdateRequest;

import java.util.List;

public interface KnowledgeBaseService {

    List<KnowledgeBaseDTO> list(String name, CommonStatus status);

    KnowledgeBaseDTO getById(Long id);

    KnowledgeBaseDTO create(KnowledgeBaseCreateRequest request);

    KnowledgeBaseDTO update(Long id, KnowledgeBaseUpdateRequest request);

    void delete(Long id);

    KnowledgeBaseDTO toggleStatus(Long id, CommonStatus status);
}
