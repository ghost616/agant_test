package com.ghost616.platform.controller;

import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.knowledge.KnowledgeFileCreateRequest;
import com.ghost616.platform.dto.knowledge.KnowledgeFileDTO;
import com.ghost616.platform.dto.knowledge.KnowledgeFileUpdateRequest;
import com.ghost616.platform.service.knowledge.KnowledgeFileService;
import com.ghost616.platform.service.knowledge.KnowledgePublishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/knowledge-bases/{kbId}/files")
@RequiredArgsConstructor
public class KnowledgeFileController {

    private final KnowledgeFileService knowledgeFileService;
    private final KnowledgePublishService knowledgePublishService;

    @GetMapping
    public ApiResponse<List<KnowledgeFileDTO>> list(@PathVariable Long kbId,
                                                    @RequestParam(required = false) String fileName,
                                                    @RequestParam(required = false) CommonStatus status) {
        List<KnowledgeFileDTO> result = knowledgeFileService.list(kbId, fileName, status);
        return ApiResponse.success(result);
    }

    @PutMapping("/refresh")
    public ApiResponse<List<KnowledgeFileDTO>> refresh(@PathVariable Long kbId,
                                                       @RequestParam(required = false) String fileName,
                                                       @RequestParam(required = false) CommonStatus status) {
        List<KnowledgeFileDTO> result = knowledgeFileService.list(kbId, fileName, status);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long kbId, @PathVariable Long id) {
        knowledgePublishService.publishFile(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeFileDTO> getById(@PathVariable Long kbId, @PathVariable Long id) {
        KnowledgeFileDTO result = knowledgeFileService.getById(id);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/content")
    public ApiResponse<String> getFileContent(@PathVariable Long kbId, @PathVariable Long id) {
        return ApiResponse.success(knowledgeFileService.getFileContent(id));
    }

    @PutMapping(value = "/{id}/content", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ApiResponse<Void> updateFileContent(@PathVariable Long kbId,
                                               @PathVariable Long id,
                                               @RequestBody String content) {
        knowledgeFileService.updateFileContent(id, content);
        return ApiResponse.success(null);
    }

    @PostMapping
    public ApiResponse<KnowledgeFileDTO> create(@PathVariable Long kbId,
                                                @Valid @RequestBody KnowledgeFileCreateRequest request) {
        KnowledgeFileDTO result = knowledgeFileService.create(kbId, request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeFileDTO> update(@PathVariable Long kbId,
                                                @PathVariable Long id,
                                                @Valid @RequestBody KnowledgeFileUpdateRequest request) {
        KnowledgeFileDTO result = knowledgeFileService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long kbId, @PathVariable Long id) {
        knowledgeFileService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<KnowledgeFileDTO> toggleStatus(@PathVariable Long kbId,
                                                      @PathVariable Long id,
                                                      @RequestParam CommonStatus status) {
        KnowledgeFileDTO result = knowledgeFileService.toggleStatus(id, status);
        return ApiResponse.success(result);
    }
}
