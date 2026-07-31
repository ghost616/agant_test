package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.evaluation.EvaluationExecutionStatusDTO;
import com.ghost616.platform.dto.evaluation.EvaluationSessionCreateResponse;
import com.ghost616.platform.entity.Evaluation;
import com.ghost616.platform.repository.EvaluationMapper;
import com.ghost616.platform.service.evaluation.EvaluationExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationSessionController {

    private final EvaluationExecutionService evaluationExecutionService;
    private final EvaluationMapper evaluationMapper;

    @PostMapping("/{id}/session")
    public ApiResponse<EvaluationSessionCreateResponse> createSession(@PathVariable Long id) {
        Evaluation evaluation = evaluationMapper.selectById(id);
        if (evaluation == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }

        EvaluationExecutionService.ExecutionSessionContext context = evaluationExecutionService.createExecutionSession(evaluation);

        List<String> userMessages = context.userMessages().stream()
                .map(m -> m.content() != null ? m.content() : "")
                .toList();

        EvaluationSessionCreateResponse response = EvaluationSessionCreateResponse.builder()
                .sessionId(context.sessionId())
                .userMessages(userMessages)
                .build();

        return ApiResponse.success(response);
    }

    @PostMapping("/{id}/session/{sessionId}/generate")
    public ApiResponse<EvaluationExecutionStatusDTO> generateResult(@PathVariable Long id, @PathVariable Long sessionId) {
        EvaluationExecutionStatusDTO status = evaluationExecutionService.generateResultAsync(id, sessionId);
        return ApiResponse.success(status);
    }

    @GetMapping("/{id}/session/{sessionId}/generate/status")
    public ApiResponse<EvaluationExecutionStatusDTO> generateStatus(@PathVariable Long id, @PathVariable Long sessionId) {
        return ApiResponse.success(evaluationExecutionService.getGenerateStatus(id, sessionId));
    }
}
