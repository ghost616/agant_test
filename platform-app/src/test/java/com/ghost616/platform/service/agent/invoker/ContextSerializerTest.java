package com.ghost616.platform.service.agent.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.invoker.ContextSerializer;


class ContextSerializerTest {

    private AgentExecutionContext context;
    private AgentExecutionContext.AgentContextMutator mutator;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mutator = new AgentExecutionContext.AgentContextMutator();
        context = new AgentExecutionContext(
                1L, 1L, "system prompt", 1L, 10,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                mutator, new HashMap<>(), new HashMap<>(), null, null);
    }

    @Test
    void stopped为false时_json包含stopped且值为false() throws Exception {
        String json = ContextSerializer.serializeToJson(context, "test-args");
        Map<String, Object> parsed = MAPPER.readValue(json, Map.class);
        Map<String, Object> contextNode = (Map<String, Object>) parsed.get("context");
        assertNotNull(contextNode);
        assertFalse((Boolean) contextNode.get("stopped"));
    }

    @Test
    void stopped为true时_json包含stopped且值为true() throws Exception {
        mutator.setStopped();
        String json = ContextSerializer.serializeToJson(context, "test-args");
        Map<String, Object> parsed = MAPPER.readValue(json, Map.class);
        Map<String, Object> contextNode = (Map<String, Object>) parsed.get("context");
        assertNotNull(contextNode);
        assertTrue((Boolean) contextNode.get("stopped"));
    }

    @Test
    void stopped字段值与isStopped一致() throws Exception {
        assertFalse(context.isStopped());
        String json1 = ContextSerializer.serializeToJson(context, "test-args");
        Map<String, Object> parsed1 = MAPPER.readValue(json1, Map.class);
        Map<String, Object> contextNode1 = (Map<String, Object>) parsed1.get("context");
        assertEquals(context.isStopped(), contextNode1.get("stopped"));

        mutator.setStopped();
        assertTrue(context.isStopped());
        String json2 = ContextSerializer.serializeToJson(context, "test-args");
        Map<String, Object> parsed2 = MAPPER.readValue(json2, Map.class);
        Map<String, Object> contextNode2 = (Map<String, Object>) parsed2.get("context");
        assertEquals(context.isStopped(), contextNode2.get("stopped"));
    }
}
