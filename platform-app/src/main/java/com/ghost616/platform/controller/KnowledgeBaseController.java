package com.ghost616.platform.controller;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeBaseUpdateRequest;
import com.ghost616.platform.service.knowledge.KnowledgeBaseService;
import com.ghost616.platform.service.knowledge.KnowledgePublishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgePublishService knowledgePublishService;

    @GetMapping
    public ApiResponse<List<KnowledgeBaseDTO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CommonStatus status) {
        List<KnowledgeBaseDTO> result = knowledgeBaseService.list(name, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> getById(@PathVariable Long id) {
        KnowledgeBaseDTO result = knowledgeBaseService.getById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseDTO> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        KnowledgeBaseDTO result = knowledgeBaseService.create(request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> update(@PathVariable Long id,
                                                @Valid @RequestBody KnowledgeBaseUpdateRequest request) {
        KnowledgeBaseDTO result = knowledgeBaseService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<KnowledgeBaseDTO> toggleStatus(@PathVariable Long id,
                                                      @RequestParam CommonStatus status) {
        KnowledgeBaseDTO result = knowledgeBaseService.toggleStatus(id, status);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/rebuild-es")
    public ApiResponse<Void> rebuildEs(@PathVariable Long id) {
        knowledgePublishService.rebuildKnowledgeBase(id);
        return ApiResponse.success(null);
    }
}
