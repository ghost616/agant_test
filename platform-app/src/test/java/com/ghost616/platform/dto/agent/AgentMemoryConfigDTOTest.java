package com.ghost616.platform.dto.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentMemoryConfigDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void agentConfigDTO_builder设置memory字段() {
        AgentConfigDTO dto = AgentConfigDTO.builder()
                .memoryEnabled(true)
                .memoryGroupCount(15)
                .build();

        assertEquals(true, dto.getMemoryEnabled());
        assertEquals(15, dto.getMemoryGroupCount());
    }

    @Test
    void agentConfigDTO_setter和getter() {
        AgentConfigDTO dto = new AgentConfigDTO();
        dto.setMemoryEnabled(false);
        dto.setMemoryGroupCount(30);

        assertEquals(false, dto.getMemoryEnabled());
        assertEquals(30, dto.getMemoryGroupCount());
    }

    @Test
    void agentConfigDTO_序列化包含memory字段() throws JsonProcessingException {
        AgentConfigDTO dto = AgentConfigDTO.builder()
                .memoryEnabled(true)
                .memoryGroupCount(12)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"memoryEnabled\":true"));
        assertTrue(json.contains("\"memoryGroupCount\":12"));
    }

    @Test
    void agentCreateRequest_builder设置memory字段() {
        AgentCreateRequest req = AgentCreateRequest.builder()
                .memoryEnabled(true)
                .memoryGroupCount(20)
                .build();

        assertEquals(true, req.getMemoryEnabled());
        assertEquals(20, req.getMemoryGroupCount());
    }

    @Test
    void agentCreateRequest_序列化包含memory字段() throws JsonProcessingException {
        AgentCreateRequest req = AgentCreateRequest.builder()
                .memoryEnabled(true)
                .memoryGroupCount(20)
                .build();

        String json = objectMapper.writeValueAsString(req);
        assertTrue(json.contains("\"memoryEnabled\":true"));
        assertTrue(json.contains("\"memoryGroupCount\":20"));
    }

    @Test
    void agentUpdateRequest_builder设置memory字段() {
        AgentUpdateRequest req = AgentUpdateRequest.builder()
                .memoryEnabled(false)
                .memoryGroupCount(5)
                .build();

        assertEquals(false, req.getMemoryEnabled());
        assertEquals(5, req.getMemoryGroupCount());
    }

    @Test
    void agentUpdateRequest_序列化包含memory字段() throws JsonProcessingException {
        AgentUpdateRequest req = AgentUpdateRequest.builder()
                .memoryEnabled(false)
                .memoryGroupCount(5)
                .build();

        String json = objectMapper.writeValueAsString(req);
        assertTrue(json.contains("\"memoryEnabled\":false"));
        assertTrue(json.contains("\"memoryGroupCount\":5"));
    }
}
