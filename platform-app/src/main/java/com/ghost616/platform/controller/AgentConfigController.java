package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.agent.AgentConfigDTO;
import com.ghost616.platform.dto.agent.AgentCreateRequest;
import com.ghost616.platform.dto.agent.AgentUpdateRequest;
import com.ghost616.platform.service.agent.AgentConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.ghost616.agentbase.enums.CommonStatus;


@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentConfigController {

    private final AgentConfigService agentConfigService;

    @GetMapping
    public ApiResponse<List<AgentConfigDTO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CommonStatus status) {
        List<AgentConfigDTO> result = agentConfigService.list(name, status);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentConfigDTO> getById(@PathVariable Long id) {
        AgentConfigDTO result = agentConfigService.getById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<AgentConfigDTO> create(@Valid @RequestBody AgentCreateRequest request) {
        AgentConfigDTO result = agentConfigService.create(request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<AgentConfigDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody AgentUpdateRequest request) {
        AgentConfigDTO result = agentConfigService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        agentConfigService.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AgentConfigDTO> toggleStatus(@PathVariable Long id,
                                                    @RequestParam CommonStatus status) {
        AgentConfigDTO result = agentConfigService.toggleStatus(id, status);
        return ApiResponse.success(result);
    }
}
