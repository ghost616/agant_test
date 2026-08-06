package com.ghost616.platform.dto.knowledge;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("KnowledgeBaseCreateRequest.name 为空白时校验失败")
    void 知识库name必填() {
        KnowledgeBaseCreateRequest r1 = KnowledgeBaseCreateRequest.builder().name(null).build();
        assertFalse(validator.validate(r1).isEmpty(), "name 为 null 应校验失败");

        KnowledgeBaseCreateRequest r2 = KnowledgeBaseCreateRequest.builder().name("").build();
        assertFalse(validator.validate(r2).isEmpty(), "name 为空字符串应校验失败");

        KnowledgeBaseCreateRequest r3 = KnowledgeBaseCreateRequest.builder().name("   ").build();
        assertFalse(validator.validate(r3).isEmpty(), "name 为空白应校验失败");

        KnowledgeBaseCreateRequest r4 = KnowledgeBaseCreateRequest.builder().name("kb").vectorModelId(1L).build();
        assertTrue(validator.validate(r4).isEmpty(), "name 非空应通过");
    }

    @Test
    @DisplayName("KnowledgeBaseCreateRequest.vectorModelId 为 null 时校验失败")
    void 知识库vectorModelId必填() {
        KnowledgeBaseCreateRequest r = KnowledgeBaseCreateRequest.builder().name("kb").build();
        Set<ConstraintViolation<KnowledgeBaseCreateRequest>> violations = validator.validate(r);
        assertFalse(violations.isEmpty(), "vectorModelId 为 null 应校验失败");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("vectorModelId")));
    }

    @Test
    @DisplayName("KnowledgeFileCreateRequest.fileName 为空白时校验失败")
    void 文件名必填() {
        KnowledgeFileCreateRequest r1 = KnowledgeFileCreateRequest.builder().fileName(null).build();
        Set<ConstraintViolation<KnowledgeFileCreateRequest>> v1 = validator.validate(r1);
        assertFalse(v1.isEmpty());
        assertTrue(v1.stream().anyMatch(v -> v.getMessage().contains("文件名称")));

        KnowledgeFileCreateRequest r2 = KnowledgeFileCreateRequest.builder().fileName("f.txt").build();
        assertTrue(validator.validate(r2).isEmpty());
    }

    @Test
    @DisplayName("KnowledgeBaseUpdateRequest 无必填约束，可只更新部分字段")
    void 更新请求可部分字段() {
        KnowledgeBaseUpdateRequest r = KnowledgeBaseUpdateRequest.builder().description("d").build();
        assertTrue(validator.validate(r).isEmpty());

        KnowledgeFileUpdateRequest f = KnowledgeFileUpdateRequest.builder().status(null).build();
        assertTrue(validator.validate(f).isEmpty());
    }
}
