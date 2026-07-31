package com.ghost616.platform.controller;

import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.evaluation.EvaluationResultDTO;
import com.ghost616.platform.service.evaluation.EvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationControllerTest {

    @Mock
    private EvaluationService evaluationService;

    @InjectMocks
    private EvaluationController controller;

    @Test
    void getResultById_shouldReturn200AndDTO() {
        EvaluationResultDTO dto = EvaluationResultDTO.builder()
                .id(500L)
                .evaluationId(700L)
                .evaluationSessionId(600L)
                .result("test result")
                .totalTokenUsed(5000L)
                .executionStatus("COMPLETED")
                .build();
        when(evaluationService.getResultById(500L)).thenReturn(dto);

        ApiResponse<EvaluationResultDTO> response = controller.getResultById(500L);

        assertTrue(response.isSuccess());
        EvaluationResultDTO data = response.getData();
        assertNotNull(data);
        assertEquals(500L, data.getId());
        assertEquals(700L, data.getEvaluationId());
        assertEquals(600L, data.getEvaluationSessionId());
        assertEquals("test result", data.getResult());
        assertEquals(5000L, data.getTotalTokenUsed());
        assertEquals("COMPLETED", data.getExecutionStatus());
    }

    @Test
    void deleteResult_shouldReturnSuccess() {
        doNothing().when(evaluationService).deleteResult(800L);

        ApiResponse<Void> response = controller.deleteResult(800L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(evaluationService).deleteResult(800L);
    }

    @Test
    void batchDeleteResults_shouldReturnSuccess() {
        doNothing().when(evaluationService).batchDeleteResults(List.of(800L, 801L));

        ApiResponse<Void> response = controller.batchDeleteResults(List.of(800L, 801L));

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(evaluationService).batchDeleteResults(List.of(800L, 801L));
    }

    @Test
    void clearResults_shouldReturnSuccess() {
        doNothing().when(evaluationService).clearResults(700L);

        ApiResponse<Void> response = controller.clearResults(700L);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        verify(evaluationService).clearResults(700L);
    }

    @Test
    void getResultById_shouldReturnApiResponseWithSuccessWhenResultFound() {
        EvaluationResultDTO dto = EvaluationResultDTO.builder()
                .id(1L)
                .evaluationId(2L)
                .evaluationSessionId(3L)
                .build();
        when(evaluationService.getResultById(1L)).thenReturn(dto);

        ApiResponse<EvaluationResultDTO> response = controller.getResultById(1L);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
    }
}
