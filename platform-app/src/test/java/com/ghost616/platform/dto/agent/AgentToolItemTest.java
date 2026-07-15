package com.ghost616.platform.dto.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.SessionAuthType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentToolItemTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 构造时正确赋值toolId和sessionAuth() {
        AgentToolItem item = new AgentToolItem(100L, SessionAuthType.PARENT);
        assertEquals(100L, item.toolId());
        assertEquals(SessionAuthType.PARENT, item.sessionAuth());
    }

    @Test
    void sessionAuth可以為null() {
        AgentToolItem item = new AgentToolItem(100L, null);
        assertEquals(100L, item.toolId());
        assertNull(item.sessionAuth());
    }

    @Test
    void toolId可以為null() {
        AgentToolItem item = new AgentToolItem(null, SessionAuthType.ALL);
        assertNull(item.toolId());
        assertEquals(SessionAuthType.ALL, item.sessionAuth());
    }

    @Test
    void toolId序列化為字符串() throws JsonProcessingException {
        AgentToolItem item = new AgentToolItem(12345L, SessionAuthType.CHILD);
        String json = objectMapper.writeValueAsString(item);
        assertTrue(json.contains("\"toolId\":\"12345\""));
    }

    @Test
    void equals和hashCode一致() {
        AgentToolItem a = new AgentToolItem(1L, SessionAuthType.ALL);
        AgentToolItem b = new AgentToolItem(1L, SessionAuthType.ALL);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals不同值不相等() {
        AgentToolItem a = new AgentToolItem(1L, SessionAuthType.ALL);
        AgentToolItem b = new AgentToolItem(2L, SessionAuthType.ALL);
        assertNotEquals(a, b);
    }

    @Test
    void AgentSkillItem構造正確() {
        AgentSkillItem item = new AgentSkillItem(200L, SessionAuthType.CHILD);
        assertEquals(200L, item.skillId());
        assertEquals(SessionAuthType.CHILD, item.sessionAuth());
    }

    @Test
    void AgentSkillItem序列化() throws JsonProcessingException {
        AgentSkillItem item = new AgentSkillItem(999L, null);
        String json = objectMapper.writeValueAsString(item);
        assertTrue(json.contains("\"skillId\":\"999\""));
    }

    @Test
    void AgentCreateRequest構建帶toolsAndSkills() {
        AgentCreateRequest req = AgentCreateRequest.builder()
                .name("test-agent")
                .description("desc")
                .systemPrompt("prompt")
                .modelId(1L)
                .recentMessageCount(10)
                .tools(List.of(new AgentToolItem(1L, SessionAuthType.ALL)))
                .skills(List.of(new AgentSkillItem(1L, SessionAuthType.PARENT)))
                .build();

        assertEquals("test-agent", req.getName());
        assertEquals(1, req.getTools().size());
        assertEquals(SessionAuthType.ALL, req.getTools().get(0).sessionAuth());
        assertEquals(1, req.getSkills().size());
        assertEquals(SessionAuthType.PARENT, req.getSkills().get(0).sessionAuth());
    }

    @Test
    void AgentCreateRequest空tools和skills() {
        AgentCreateRequest req = AgentCreateRequest.builder()
                .name("test")
                .tools(null)
                .skills(null)
                .build();
        assertNull(req.getTools());
        assertNull(req.getSkills());
    }

    @Test
    void AgentUpdateRequest構建帶toolsAndSkills() {
        AgentUpdateRequest req = AgentUpdateRequest.builder()
                .name("updated-agent")
                .tools(List.of(new AgentToolItem(2L, SessionAuthType.CHILD)))
                .skills(List.of(new AgentSkillItem(2L, SessionAuthType.ALL)))
                .build();

        assertEquals("updated-agent", req.getName());
        assertEquals(1, req.getTools().size());
        assertEquals(SessionAuthType.CHILD, req.getTools().get(0).sessionAuth());
    }

    @Test
    void AgentConfigDTO包含toolsAndSkills() {
        AgentConfigDTO dto = AgentConfigDTO.builder()
                .id(1L)
                .name("dto-agent")
                .tools(List.of(new AgentToolItem(10L, SessionAuthType.ALL)))
                .skills(List.of(new AgentSkillItem(20L, SessionAuthType.PARENT)))
                .build();

        assertEquals(1, dto.getTools().size());
        assertEquals(10L, dto.getTools().get(0).toolId());
        assertEquals(SessionAuthType.ALL, dto.getTools().get(0).sessionAuth());
        assertEquals(20L, dto.getSkills().get(0).skillId());
        assertEquals(SessionAuthType.PARENT, dto.getSkills().get(0).sessionAuth());
    }
}
