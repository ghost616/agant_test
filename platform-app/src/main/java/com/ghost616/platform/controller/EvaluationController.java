package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.evaluation.EvaluationCreateRequest;
import com.ghost616.platform.dto.evaluation.EvaluationDTO;
import com.ghost616.platform.dto.evaluation.EvaluationResultDTO;
import com.ghost616.platform.dto.evaluation.EvaluationUpdateRequest;
import com.ghost616.platform.service.evaluation.EvaluationService;
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
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping
    public ApiResponse<List<EvaluationDTO>> list() {
        List<EvaluationDTO> result = evaluationService.list();
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/results")
    public ApiResponse<List<EvaluationResultDTO>> listResults(@PathVariable Long id) {
        List<EvaluationResultDTO> result = evaluationService.listResults(id);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<EvaluationDTO> getById(@PathVariable Long id) {
        EvaluationDTO result = evaluationService.getById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<EvaluationDTO> create(@Valid @RequestBody EvaluationCreateRequest request) {
        EvaluationDTO result = evaluationService.create(request);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<EvaluationDTO> update(@PathVariable Long id, @Valid @RequestBody EvaluationUpdateRequest request) {
        EvaluationDTO result = evaluationService.update(id, request);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        evaluationService.delete(id);
        return ApiResponse.success(null);
    }
}
