package com.ghost616.platform.service.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.agentbase.enums.LogLevel;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.log.ContextBuildLogData;
import com.ghost616.agentbase.service.agent.log.ContextLogData;
import com.ghost616.agentbase.service.agent.log.ModelCallLogData;
import com.ghost616.agentbase.service.agent.log.RequestEntryLogData;
import com.ghost616.platform.entity.AgentLogEntity;
import com.ghost616.platform.repository.AgentLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseAgentLogTest {

    @Mock
    private AgentLogMapper agentLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DatabaseAgentLog databaseAgentLog;

    @BeforeEach
    void setUp() {
        databaseAgentLog = new DatabaseAgentLog(agentLogMapper, objectMapper);
    }

    private AgentExecutionContext buildContext(String sessionId, String conversationId) {
        return new AgentExecutionContext(
                sessionId, "agent-1", "prompt", "model-1", 10,
                List.of(), List.of(), List.of(),
                new AgentExecutionContext.AgentContextMutator(),
                new HashMap<>(), new HashMap<>(),
                null, "proj", List.of(), conversationId);
    }

    @Test
    void addLog_RequestEntryLogData_正确提取context字段并写入() {
        AgentExecutionContext context = buildContext("123", "conv-1");
        RequestEntryLogData logData = RequestEntryLogData.builder()
                .context(context)
                .sessionId("123")
                .modelId("model-1")
                .content("hello")
                .isToolContinue(false)
                .logLevel(LogLevel.INFO)
                .build();

        databaseAgentLog.addLog(logData);

        ArgumentCaptor<AgentLogEntity> captor = ArgumentCaptor.forClass(AgentLogEntity.class);
        verify(agentLogMapper).insert(captor.capture());
        AgentLogEntity entity = captor.getValue();
        assertEquals(123L, entity.getSessionId());
        assertEquals("conv-1", entity.getConversationId());
        assertEquals("REQUEST_ENTRY", entity.getLogType());
        assertEquals("INFO", entity.getLogLevel());
        assertNotNull(entity.getLogData());
        assertTrue(entity.getLogData().contains("\"sessionId\":\"123\""));
    }

    @Test
    void addLog_ModelCallLogData_logType存枚举code且logData为JSON() {
        AgentExecutionContext context = buildContext("456", "conv-2");
        ModelCallLogData logData = ModelCallLogData.builder()
                .context(context)
                .messageCount(3)
                .toolCount(2)
                .toolNames(List.of("tool-a"))
                .thinking(true)
                .logLevel(LogLevel.WARN)
                .build();

        databaseAgentLog.addLog(logData);

        ArgumentCaptor<AgentLogEntity> captor = ArgumentCaptor.forClass(AgentLogEntity.class);
        verify(agentLogMapper).insert(captor.capture());
        AgentLogEntity entity = captor.getValue();
        assertEquals(456L, entity.getSessionId());
        assertEquals("conv-2", entity.getConversationId());
        assertEquals("MODEL_CALL", entity.getLogType());
        assertEquals("WARN", entity.getLogLevel());
        assertNotNull(entity.getLogData());
        assertTrue(entity.getLogData().contains("\"messageCount\":3"));
    }

    @Test
    void addLog_context为null_不设置会话字段仍可写入() {
        RequestEntryLogData logData = RequestEntryLogData.builder()
                .context(null)
                .sessionId("123")
                .modelId("model-1")
                .content("hello")
                .isToolContinue(false)
                .logLevel(LogLevel.INFO)
                .build();

        databaseAgentLog.addLog(logData);

        ArgumentCaptor<AgentLogEntity> captor = ArgumentCaptor.forClass(AgentLogEntity.class);
        verify(agentLogMapper).insert(captor.capture());
        AgentLogEntity entity = captor.getValue();
        assertNull(entity.getSessionId());
        assertNull(entity.getConversationId());
        assertEquals("REQUEST_ENTRY", entity.getLogType());
        assertNotNull(entity.getLogData());
    }

    @Test
    void addLog_sessionId非数字_解析失败返回null不抛异常() {
        AgentExecutionContext context = buildContext("abc", "conv-3");
        RequestEntryLogData logData = RequestEntryLogData.builder()
                .context(context)
                .sessionId("abc")
                .modelId("model-1")
                .content("hello")
                .isToolContinue(false)
                .logLevel(LogLevel.INFO)
                .build();

        assertDoesNotThrow(() -> databaseAgentLog.addLog(logData));

        ArgumentCaptor<AgentLogEntity> captor = ArgumentCaptor.forClass(AgentLogEntity.class);
        verify(agentLogMapper).insert(captor.capture());
        AgentLogEntity entity = captor.getValue();
        assertNull(entity.getSessionId());
        assertEquals("conv-3", entity.getConversationId());
        assertNotNull(entity.getLogData());
    }

    @Test
    void addLog_非ContextLogData_不提取context字段仍可写入() {
        com.ghost616.agentbase.service.agent.log.LogData logData = ContextBuildLogData.builder()
                .sessionId("s-1")
                .agentId("a-1")
                .modelId("m-1")
                .toolCount(2)
                .historyCount(5)
                .isSubSession(false)
                .cacheHit(false)
                .sessionVariables(Map.of())
                .logLevel(LogLevel.ERROR)
                .build();
        assertFalse(logData instanceof ContextLogData);

        databaseAgentLog.addLog(logData);

        ArgumentCaptor<AgentLogEntity> captor = ArgumentCaptor.forClass(AgentLogEntity.class);
        verify(agentLogMapper).insert(captor.capture());
        AgentLogEntity entity = captor.getValue();
        assertNull(entity.getSessionId());
        assertNull(entity.getConversationId());
        assertEquals("CONTEXT_BUILD", entity.getLogType());
        assertEquals("ERROR", entity.getLogLevel());
        assertNotNull(entity.getLogData());
    }
}
