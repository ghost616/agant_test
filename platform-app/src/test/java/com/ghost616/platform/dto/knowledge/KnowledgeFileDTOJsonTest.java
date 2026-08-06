package com.ghost616.platform.dto.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.CommonStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeFileDTOJsonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    @DisplayName("KnowledgeFileDTO.id/knowledgeBaseId 使用 ToStringSerializer 序列化为字符串")
    void id序列化为字符串() throws Exception {
        KnowledgeFileDTO dto = KnowledgeFileDTO.builder()
                .id(111L)
                .knowledgeBaseId(222L)
                .fileName("a.txt")
                .fileSize(10L)
                .lineCount(2)
                .status(CommonStatus.ENABLED)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"id\":\"111\""), "id 应序列化为字符串, 实际: " + json);
        assertTrue(json.contains("\"knowledgeBaseId\":\"222\""), "knowledgeBaseId 应序列化为字符串, 实际: " + json);
        assertFalse(json.contains("fileContent"), "DTO 不应包含 fileContent 字段, 实际: " + json);
    }

    @Test
    @DisplayName("KnowledgeFileDTO 反序列化")
    void 反序列化() throws Exception {
        String json = "{\"id\":\"1\",\"knowledgeBaseId\":\"2\",\"fileName\":\"a.txt\",\"status\":\"ENABLED\"}";
        KnowledgeFileDTO dto = objectMapper.readValue(json, KnowledgeFileDTO.class);
        assertEquals(1L, dto.getId());
        assertEquals(2L, dto.getKnowledgeBaseId());
        assertEquals("a.txt", dto.getFileName());
    }
}
