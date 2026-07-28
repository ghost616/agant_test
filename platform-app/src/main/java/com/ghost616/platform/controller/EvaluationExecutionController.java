package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import com.ghost616.platform.service.evaluation.EvaluationExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationExecutionController {

    private final EvaluationExecutionService evaluationExecutionService;

    @PostMapping("/{id}/execute")
    public ApiResponse<EvaluationExecutionStatusDTO> execute(@PathVariable Long id) {
        EvaluationExecutionStatusDTO result = evaluationExecutionService.execute(id);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/execute/status")
    public ApiResponse<EvaluationExecutionStatusDTO> getStatus(@PathVariable Long id) {
        EvaluationExecutionStatusDTO result = evaluationExecutionService.getStatus(id);
        return ApiResponse.success(result);
    }
}
