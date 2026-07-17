package com.ghost616.platform.dto.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubSessionDataDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void builderShouldSetThinkingField() {
        SubSessionDataDTO dto = SubSessionDataDTO.builder()
                .childSessionId(100L)
                .userMessage("hello")
                .thinking(true)
                .build();

        assertEquals(100L, dto.getChildSessionId());
        assertEquals("hello", dto.getUserMessage());
        assertTrue(dto.getThinking());
    }

    @Test
    void builderWithThinkingNull() {
        SubSessionDataDTO dto = SubSessionDataDTO.builder()
                .childSessionId(200L)
                .userMessage("test")
                .build();

        assertNull(dto.getThinking());
    }

    @Test
    void builderWithThinkingFalse() {
        SubSessionDataDTO dto = SubSessionDataDTO.builder()
                .childSessionId(300L)
                .userMessage("no thinking")
                .thinking(false)
                .build();

        assertFalse(dto.getThinking());
    }

    @Test
    void noArgsConstructorShouldSetDefaultsToNull() {
        SubSessionDataDTO dto = new SubSessionDataDTO();

        assertNull(dto.getChildSessionId());
        assertNull(dto.getUserMessage());
        assertNull(dto.getThinking());
    }

    @Test
    void allArgsConstructorShouldSetAllFields() {
        SubSessionDataDTO dto = new SubSessionDataDTO(100L, "hello", true);

        assertEquals(100L, dto.getChildSessionId());
        assertEquals("hello", dto.getUserMessage());
        assertTrue(dto.getThinking());
    }

    @Test
    void allArgsConstructorWithNullThinking() {
        SubSessionDataDTO dto = new SubSessionDataDTO(200L, "test", null);

        assertEquals(200L, dto.getChildSessionId());
        assertEquals("test", dto.getUserMessage());
        assertNull(dto.getThinking());
    }

    @Test
    void allArgsConstructorWithFalseThinking() {
        SubSessionDataDTO dto = new SubSessionDataDTO(300L, "no", false);

        assertFalse(dto.getThinking());
    }

    @Test
    void setterShouldUpdateThinking() {
        SubSessionDataDTO dto = new SubSessionDataDTO();
        dto.setThinking(true);

        assertTrue(dto.getThinking());
    }

    @Test
    void serializationShouldIncludeThinkingField() throws JsonProcessingException {
        SubSessionDataDTO dto = SubSessionDataDTO.builder()
                .childSessionId(100L)
                .userMessage("hello")
                .thinking(true)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"thinking\":true"));
        assertTrue(json.contains("\"childSessionId\":\"100\""));
        assertTrue(json.contains("\"userMessage\":\"hello\""));
    }

    @Test
    void serializationWithThinkingNull() throws JsonProcessingException {
        SubSessionDataDTO dto = SubSessionDataDTO.builder()
                .childSessionId(200L)
                .userMessage("test")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"childSessionId\":\"200\""));
        assertTrue(json.contains("\"thinking\":null"));
    }

    @Test
    void deserializationShouldIncludeThinkingField() throws JsonProcessingException {
        String json = "{\"childSessionId\":\"100\",\"userMessage\":\"hello\",\"thinking\":true}";

        SubSessionDataDTO dto = objectMapper.readValue(json, SubSessionDataDTO.class);

        assertEquals(100L, dto.getChildSessionId());
        assertEquals("hello", dto.getUserMessage());
        assertTrue(dto.getThinking());
    }

    @Test
    void deserializationWithThinkingFalse() throws JsonProcessingException {
        String json = "{\"childSessionId\":\"300\",\"userMessage\":\"no\",\"thinking\":false}";

        SubSessionDataDTO dto = objectMapper.readValue(json, SubSessionDataDTO.class);

        assertFalse(dto.getThinking());
    }

    @Test
    void deserializationWithoutThinking() throws JsonProcessingException {
        String json = "{\"childSessionId\":\"200\",\"userMessage\":\"test\"}";

        SubSessionDataDTO dto = objectMapper.readValue(json, SubSessionDataDTO.class);

        assertEquals(200L, dto.getChildSessionId());
        assertEquals("test", dto.getUserMessage());
        assertNull(dto.getThinking());
    }

    @Test
    void equalsAndHashCodeConsistency() {
        SubSessionDataDTO dto1 = SubSessionDataDTO.builder()
                .childSessionId(100L).userMessage("msg").thinking(true).build();
        SubSessionDataDTO dto2 = SubSessionDataDTO.builder()
                .childSessionId(100L).userMessage("msg").thinking(true).build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void equalsWithDifferentThinking() {
        SubSessionDataDTO dto1 = SubSessionDataDTO.builder()
                .childSessionId(100L).userMessage("msg").thinking(true).build();
        SubSessionDataDTO dto2 = SubSessionDataDTO.builder()
                .childSessionId(100L).userMessage("msg").thinking(false).build();

        assertNotEquals(dto1, dto2);
    }
}
