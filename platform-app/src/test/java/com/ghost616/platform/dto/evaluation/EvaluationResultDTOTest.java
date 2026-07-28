package com.ghost616.platform.dto.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationResultDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void builderShouldCreateDTOWithAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 12, 0, 0);
        EvaluationResultDTO dto = EvaluationResultDTO.builder()
                .id(1L)
                .evaluationId(10L)
                .evaluationSessionId(100L)
                .result("评估结果")
                .totalTokenUsed(500L)
                .executionStatus("COMPLETED")
                .createTime(now)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getEvaluationId());
        assertEquals(100L, dto.getEvaluationSessionId());
        assertEquals("评估结果", dto.getResult());
        assertEquals(500L, dto.getTotalTokenUsed());
        assertEquals("COMPLETED", dto.getExecutionStatus());
        assertEquals(now, dto.getCreateTime());
    }

    @Test
    void noArgsConstructorShouldCreateEmptyDTO() {
        EvaluationResultDTO dto = new EvaluationResultDTO();
        assertNull(dto.getId());
        assertNull(dto.getEvaluationId());
        assertNull(dto.getEvaluationSessionId());
        assertNull(dto.getResult());
        assertNull(dto.getTotalTokenUsed());
        assertNull(dto.getExecutionStatus());
        assertNull(dto.getCreateTime());
    }

    @Test
    void allArgsConstructorShouldSetAllFields() {
        LocalDateTime now = LocalDateTime.now();
        EvaluationResultDTO dto = new EvaluationResultDTO(1L, 10L, 100L, "result", 500L, "DONE", now);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getEvaluationId());
        assertEquals(100L, dto.getEvaluationSessionId());
        assertEquals("result", dto.getResult());
        assertEquals(500L, dto.getTotalTokenUsed());
        assertEquals("DONE", dto.getExecutionStatus());
        assertEquals(now, dto.getCreateTime());
    }

    @Test
    void setterShouldUpdateFields() {
        EvaluationResultDTO dto = new EvaluationResultDTO();
        dto.setId(2L);
        dto.setEvaluationId(20L);
        dto.setEvaluationSessionId(200L);
        dto.setResult("new result");
        dto.setTotalTokenUsed(1000L);
        dto.setExecutionStatus("FAILED");

        assertEquals(2L, dto.getId());
        assertEquals(20L, dto.getEvaluationId());
        assertEquals(200L, dto.getEvaluationSessionId());
        assertEquals("new result", dto.getResult());
        assertEquals(1000L, dto.getTotalTokenUsed());
        assertEquals("FAILED", dto.getExecutionStatus());
    }

    @Test
    void jsonSerializationShouldOutputLongFieldsAsStrings() throws Exception {
        EvaluationResultDTO dto = EvaluationResultDTO.builder()
                .id(1L)
                .evaluationId(10L)
                .evaluationSessionId(100L)
                .result("测试结果")
                .totalTokenUsed(500L)
                .executionStatus("COMPLETED")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"id\":\"1\""));
        assertTrue(json.contains("\"evaluationId\":\"10\""));
        assertTrue(json.contains("\"evaluationSessionId\":\"100\""));
        assertTrue(json.contains("\"totalTokenUsed\":\"500\""));
        assertTrue(json.contains("\"result\":\"测试结果\""));
        assertTrue(json.contains("\"executionStatus\":\"COMPLETED\""));
    }

    @Test
    void equalsAndHashCode() {
        LocalDateTime now = LocalDateTime.now();
        EvaluationResultDTO dto1 = new EvaluationResultDTO(1L, 10L, 100L, "r", 500L, "OK", now);
        EvaluationResultDTO dto2 = new EvaluationResultDTO(1L, 10L, 100L, "r", 500L, "OK", now);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
