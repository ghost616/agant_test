package com.ghost616.agentbase.service.agent.log;

import com.ghost616.agentbase.enums.LogType;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryExpandLogDataTest {

    @Test
    void logType应返回HISTORY_EXPAND() {
        HistoryExpandLogData data = HistoryExpandLogData.builder().build();
        assertEquals(LogType.HISTORY_EXPAND, data.logType());
    }

    @Test
    void 数字字段应默认为0() {
        HistoryExpandLogData data = HistoryExpandLogData.builder().build();
        assertEquals(0, data.getFoldedCount());
        assertNull(data.getExpandedMessages());
    }

    @Test
    void builder应正确设置继承与自有字段() {
        AgentExecutionContext context = new AgentExecutionContext(
                "s1", "agent-1", "sys_prompt", "model-1", null,
                new ArrayList<>(), new ArrayList<>(), null,
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(), null, "", null, null);
        List<String> messages = new ArrayList<>(List.of("【历史消息组0】完整内容如下：\nuser: q0", "【历史消息组2】完整内容如下：\nuser: q2"));
        HistoryExpandLogData data = HistoryExpandLogData.builder()
                .context(context)
                .foldedCount(10)
                .expandedMessages(messages)
                .build();

        assertSame(context, data.getContext());
        assertEquals(10, data.getFoldedCount());
        assertEquals(messages, data.getExpandedMessages());
    }
}
