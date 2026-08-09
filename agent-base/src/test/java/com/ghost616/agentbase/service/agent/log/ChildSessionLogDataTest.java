package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChildSessionLogDataTest {

    @Test
    void logType应返回CHILD_SESSION() {
        ChildSessionLogData data = ChildSessionLogData.builder().build();
        assertEquals(LogType.CHILD_SESSION, data.logType());
    }

    @Test
    void 字段应默认为null() {
        ChildSessionLogData data = ChildSessionLogData.builder().build();
        assertNull(data.getContext());
        assertNull(data.getLogLevel());
        assertNull(data.getParentSessionId());
        assertNull(data.getChildSessionId());
        assertNull(data.getSessionName());
        assertNull(data.getDescription());
        assertNull(data.getModelId());
        assertNull(data.getToolIds());
        assertNull(data.getSkillIds());
        assertNull(data.getPrompt());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        ChildSessionLogData data = ChildSessionLogData.builder()
                .parentSessionId("p1")
                .childSessionId("c1")
                .sessionName("子会话")
                .description("子会话描述")
                .modelId("300")
                .toolIds(List.of("t1", "t2"))
                .skillIds(List.of("s1"))
                .prompt("子会话提示词")
                .build();

        assertEquals("p1", data.getParentSessionId());
        assertEquals("c1", data.getChildSessionId());
        assertEquals("子会话", data.getSessionName());
        assertEquals("子会话描述", data.getDescription());
        assertEquals("300", data.getModelId());
        assertEquals(List.of("t1", "t2"), data.getToolIds());
        assertEquals(List.of("s1"), data.getSkillIds());
        assertEquals("子会话提示词", data.getPrompt());
    }
}
