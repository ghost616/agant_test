package com.ghost616.platform.dto.agent_log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AgentLogDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void builder构造_含sessionName字段() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 9, 10, 30);
        AgentLogDTO dto = AgentLogDTO.builder()
                .id(100L)
                .sessionId(200L)
                .sessionName("测试会话")
                .conversationId("conv-1")
                .logType("MODEL_CALL")
                .logLevel("INFO")
                .logData("{\"k\":1}")
                .createTime(now)
                .build();

        assertEquals(100L, dto.getId());
        assertEquals(200L, dto.getSessionId());
        assertEquals("测试会话", dto.getSessionName());
        assertEquals("conv-1", dto.getConversationId());
        assertEquals("MODEL_CALL", dto.getLogType());
        assertEquals("INFO", dto.getLogLevel());
        assertEquals("{\"k\":1}", dto.getLogData());
        assertEquals(now, dto.getCreateTime());
    }

    @Test
    void builder构造_sessionName可为null() {
        AgentLogDTO dto = AgentLogDTO.builder()
                .id(1L)
                .sessionId(2L)
                .build();

        assertNull(dto.getSessionName());
    }

    @Test
    void builder构造_含sessionVariables和conversationVariables字段() {
        AgentLogDTO dto = AgentLogDTO.builder()
                .id(1L)
                .sessionId(2L)
                .sessionName("会话")
                .sessionVariables("{\"skill\":\"java\"}")
                .conversationVariables("{\"topic\":\"log\"}")
                .build();

        assertEquals("{\"skill\":\"java\"}", dto.getSessionVariables());
        assertEquals("{\"topic\":\"log\"}", dto.getConversationVariables());
    }

    @Test
    void builder构造_sessionVariables可为null() {
        AgentLogDTO dto = AgentLogDTO.builder()
                .id(1L)
                .sessionId(2L)
                .build();

        assertNull(dto.getSessionVariables());
        assertNull(dto.getConversationVariables());
    }

    @Test
    void 序列化包含变量字段() throws Exception {
        AgentLogDTO dto = AgentLogDTO.builder()
                .id(1L)
                .sessionId(2L)
                .sessionVariables("{\"skill\":\"java\"}")
                .conversationVariables("{\"topic\":\"log\"}")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("sessionVariables"), "应包含 sessionVariables: " + json);
        assertTrue(json.contains("conversationVariables"), "应包含 conversationVariables: " + json);
    }

    @Test
    void id序列化为字符串() throws Exception {
        AgentLogDTO dto = AgentLogDTO.builder()
                .id(1234567890123456789L)
                .sessionId(987654321L)
                .sessionName("会话")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertTrue(json.contains("\"id\":\"1234567890123456789\""), "id 应序列化为字符串: " + json);
        assertTrue(json.contains("\"sessionId\":\"987654321\""), "sessionId 应序列化为字符串: " + json);
    }

    @Test
    void JSON反序列化_id为字符串() throws Exception {
        String json = "{\"id\":\"123\",\"sessionId\":\"456\",\"sessionName\":\"会话\",\"logType\":\"ROUTE\"}";
        AgentLogDTO dto = objectMapper.readValue(json, AgentLogDTO.class);
        assertEquals(123L, dto.getId());
        assertEquals(456L, dto.getSessionId());
        assertEquals("会话", dto.getSessionName());
        assertEquals("ROUTE", dto.getLogType());
    }
}
