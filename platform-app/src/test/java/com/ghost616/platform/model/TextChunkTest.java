package com.ghost616.platform.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("字段完整性：知识块模型包含全部 7 个字段")
    void 字段完整性() throws Exception {
        TextChunk chunk = TextChunk.builder()
                .knowledgeBaseId(100L)
                .fileId(200L)
                .lineNumber(3)
                .vector(List.of(0.1f, 0.2f))
                .text("文本内容")
                .kbEnabled(true)
                .fileEnabled(false)
                .build();

        assertEquals(100L, chunk.getKnowledgeBaseId());
        assertEquals(200L, chunk.getFileId());
        assertEquals(3, chunk.getLineNumber());
        assertEquals(List.of(0.1f, 0.2f), chunk.getVector());
        assertEquals("文本内容", chunk.getText());
        assertEquals(Boolean.TRUE, chunk.getKbEnabled());
        assertEquals(Boolean.FALSE, chunk.getFileEnabled());

        String json = objectMapper.writeValueAsString(chunk);
        assertTrue(json.contains("\"knowledgeBaseId\""), "json 应包含 knowledgeBaseId: " + json);
        assertTrue(json.contains("\"fileId\""), "json 应包含 fileId: " + json);
        assertTrue(json.contains("\"lineNumber\""), "json 应包含 lineNumber: " + json);
        assertTrue(json.contains("\"vector\""), "json 应包含 vector: " + json);
        assertTrue(json.contains("\"text\""), "json 应包含 text: " + json);
        assertTrue(json.contains("\"kbEnabled\""), "json 应包含 kbEnabled: " + json);
        assertTrue(json.contains("\"fileEnabled\""), "json 应包含 fileEnabled: " + json);
    }

    @Test
    @DisplayName("knowledgeBaseId/fileId 使用 ToStringSerializer 序列化为字符串")
    void id序列化为字符串() throws Exception {
        TextChunk chunk = TextChunk.builder()
                .knowledgeBaseId(100L)
                .fileId(200L)
                .build();

        String json = objectMapper.writeValueAsString(chunk);
        assertTrue(json.contains("\"knowledgeBaseId\":\"100\""),
                "knowledgeBaseId 应序列化为字符串, 实际: " + json);
        assertTrue(json.contains("\"fileId\":\"200\""),
                "fileId 应序列化为字符串, 实际: " + json);
    }

    @Test
    @DisplayName("Lombok Builder/Data 可用：setter 与 builder 均可访问")
    void lombok可用() {
        TextChunk chunk = new TextChunk();
        chunk.setKnowledgeBaseId(1L);
        chunk.setFileId(2L);
        chunk.setLineNumber(1);
        chunk.setText("t");
        chunk.setKbEnabled(true);
        chunk.setFileEnabled(false);

        assertEquals(1L, chunk.getKnowledgeBaseId());
        assertEquals(2L, chunk.getFileId());
        assertEquals(1, chunk.getLineNumber());
        assertEquals("t", chunk.getText());
        assertTrue(chunk.getKbEnabled());
        assertFalse(chunk.getFileEnabled());

        TextChunk built = TextChunk.builder().knowledgeBaseId(3L).build();
        assertEquals(3L, built.getKnowledgeBaseId());
        assertNull(built.getVector());
    }
}
