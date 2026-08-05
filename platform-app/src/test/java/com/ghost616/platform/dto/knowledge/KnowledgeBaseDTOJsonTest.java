package com.ghost616.platform.dto.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.CommonStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseDTOJsonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    @DisplayName("KnowledgeBaseDTO.id 使用 ToStringSerializer 序列化为字符串")
    void id序列化为字符串() throws Exception {
        KnowledgeBaseDTO dto = KnowledgeBaseDTO.builder()
                .id(1234567890123456789L)
                .name("kb")
                .status(CommonStatus.ENABLED)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"id\":\"1234567890123456789\""), "id 应序列化为字符串, 实际: " + json);
    }

    @Test
    @DisplayName("KnowledgeBaseDTO 反序列化")
    void 反序列化() throws Exception {
        String json = "{\"id\":\"123\",\"name\":\"kb\",\"status\":\"ENABLED\"}";
        KnowledgeBaseDTO dto = objectMapper.readValue(json, KnowledgeBaseDTO.class);
        assertEquals(123L, dto.getId());
        assertEquals("kb", dto.getName());
        assertEquals(CommonStatus.ENABLED, dto.getStatus());
    }
}
