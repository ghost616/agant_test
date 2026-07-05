package com.ghost616.platform.service.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import com.ghost616.agentbase.dto.skill.SkillConfigDTO;
import com.ghost616.agentbase.dto.tool.ToolConfigDTO;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;


@ExtendWith(MockitoExtension.class)
class AgentExecutionContextTest {

    private AgentExecutionContext context;
    private AgentExecutionContext.AgentContextMutator mutator;

    @BeforeEach
    void setUp() {
        mutator = new AgentExecutionContext.AgentContextMutator();
        context = new AgentExecutionContext(
                1L, 1L, "system prompt", 1L, 10,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                mutator, new HashMap<>(), new HashMap<>());
    }

    @Test
    void isStopped_默认返回false() {
        assertFalse(context.isStopped());
    }

    @Test
    void setStopped_后isStopped返回true() {
        mutator.setStopped();
        assertTrue(context.isStopped());
    }

    @Test
    void resetStopped_后isStopped返回false() {
        mutator.setStopped();
        assertTrue(context.isStopped());
        mutator.resetStopped();
        assertFalse(context.isStopped());
    }

    @Test
    void resetStopped_未设置时调用仍返回false() {
        mutator.resetStopped();
        assertFalse(context.isStopped());
    }

    @Test
    void setStopped_多次调用仍返回true() {
        mutator.setStopped();
        mutator.setStopped();
        assertTrue(context.isStopped());
    }

    @Test
    void setStopped_resetStopped_可重复切换() {
        mutator.setStopped();
        assertTrue(context.isStopped());
        mutator.resetStopped();
        assertFalse(context.isStopped());
        mutator.setStopped();
        assertTrue(context.isStopped());
        mutator.resetStopped();
        assertFalse(context.isStopped());
    }
}
