package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.skill.SkillConfigDTO;
import com.ghost616.platform.dto.skill.SkillCreateRequest;
import com.ghost616.platform.dto.skill.SkillUpdateRequest;
import com.ghost616.platform.enums.CommonStatus;
import com.ghost616.platform.service.skill.SkillConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillConfigController {

    private final SkillConfigService skillConfigService;

    @GetMapping
    public ApiResponse<List<SkillConfigDTO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CommonStatus status) {
        List<SkillConfigDTO> result = skillConfigService.list(name, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillConfigDTO> getById(@PathVariable Long id) {
        SkillConfigDTO result = skillConfigService.getById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<SkillConfigDTO> create(@Valid @RequestBody SkillCreateRequest request) {
        SkillConfigDTO result = skillConfigService.create(request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<SkillConfigDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody SkillUpdateRequest request) {
        SkillConfigDTO result = skillConfigService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        skillConfigService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<SkillConfigDTO> toggleStatus(@PathVariable Long id,
                                                    @RequestParam CommonStatus status) {
        SkillConfigDTO result = skillConfigService.toggleStatus(id, status);
        return ApiResponse.success(result);
    }
}
