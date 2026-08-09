package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkillLoadLogDataTest {

    @Test
    void logType应返回SKILL_LOAD() {
        SkillLoadLogData data = SkillLoadLogData.builder().build();
        assertEquals(LogType.SKILL_LOAD, data.logType());
    }

    @Test
    void 数字字段应默认为0() {
        SkillLoadLogData data = SkillLoadLogData.builder().build();
        assertEquals(0, data.getSkillCount());
        assertNull(data.getSkillNames());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        List<String> skillNames = new ArrayList<>(List.of("skill-a", "skill-b"));
        SkillLoadLogData data = SkillLoadLogData.builder()
                .context(context)
                .skillNames(skillNames)
                .skillCount(2)
                .build();

        assertSame(context, data.getContext());
        assertEquals(skillNames, data.getSkillNames());
        assertEquals(2, data.getSkillCount());
    }
}
