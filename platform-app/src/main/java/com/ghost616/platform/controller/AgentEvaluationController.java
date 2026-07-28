package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.agent_evaluation.AgentEvaluationCreateRequest;
import com.ghost616.platform.dto.agent_evaluation.AgentEvaluationDTO;
import com.ghost616.platform.dto.agent_evaluation.AgentEvaluationUpdateRequest;
import com.ghost616.platform.service.agent_evaluation.AgentEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent-evaluations")
@RequiredArgsConstructor
public class AgentEvaluationController {

    private final AgentEvaluationService agentEvaluationService;

    @GetMapping
    public ApiResponse<List<AgentEvaluationDTO>> list() {
        List<AgentEvaluationDTO> result = agentEvaluationService.list();
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentEvaluationDTO> getById(@PathVariable Long id) {
        AgentEvaluationDTO result = agentEvaluationService.getById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<AgentEvaluationDTO> create(@Valid @RequestBody AgentEvaluationCreateRequest request) {
        AgentEvaluationDTO result = agentEvaluationService.create(request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<AgentEvaluationDTO> update(@PathVariable Long id, @Valid @RequestBody AgentEvaluationUpdateRequest request) {
        AgentEvaluationDTO result = agentEvaluationService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        agentEvaluationService.delete(id);
        return ApiResponse.success(null);
    }
}
