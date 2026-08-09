package com.ghost616.agentbase.core;

import com.ghost616.agentbase.service.agent.log.AgentLog;
import com.ghost616.agentbase.service.agent.log.LogData;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentComponentRegistryTest {

    private AgentComponentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentComponentRegistry();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void getAgentLog未设置时应直接返回null() {
        assertNull(registry.getAgentLog());
    }

    @Test
    void setAgentLog后getAgentLog应返回设置实例() {
        AgentLog agentLog = new AgentLog() {
            @Override
            public void addLog(AgentExecutionContext context, LogData logData) {
            }
        };
        registry.setAgentLog(agentLog);
        assertSame(agentLog, registry.getAgentLog());
    }
}
