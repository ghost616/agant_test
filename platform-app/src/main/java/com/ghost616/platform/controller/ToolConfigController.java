package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.tool.ToolCreateRequest;
import com.ghost616.platform.dto.tool.ToolUpdateRequest;
import com.ghost616.platform.service.tool.ToolConfigService;
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

import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.enums.CommonStatus;
import com.ghost616.agentbase.enums.ToolType;


@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolConfigController {

    private final ToolConfigService toolConfigService;

    @GetMapping
    public ApiResponse<List<ToolConfigDTO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ToolType toolType,
            @RequestParam(required = false) CommonStatus status) {
        List<ToolConfigDTO> result = toolConfigService.list(name, toolType, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<ToolConfigDTO> getById(@PathVariable Long id) {
        ToolConfigDTO result = toolConfigService.getById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<ToolConfigDTO> create(@Valid @RequestBody ToolCreateRequest request) {
        ToolConfigDTO result = toolConfigService.create(request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<ToolConfigDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody ToolUpdateRequest request) {
        ToolConfigDTO result = toolConfigService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        toolConfigService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{name}/impl")
    public ApiResponse<ToolConfigDTO> getImplByName(@PathVariable String name) {
        ToolConfigDTO result = toolConfigService.getImplByName(name);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ToolConfigDTO> toggleStatus(@PathVariable Long id,
                                                   @RequestParam CommonStatus status) {
        ToolConfigDTO result = toolConfigService.toggleStatus(id, status);
        return ApiResponse.success(result);
    }
}
