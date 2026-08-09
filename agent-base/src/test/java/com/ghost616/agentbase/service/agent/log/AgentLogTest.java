package com.ghost616.agentbase.service.agent.log;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentLogTest {

    @Test
    void addLog应能被实现并被调用() {
        LogData logData = RequestEntryLogData.builder().build();
        final boolean[] called = {false};
        AgentLog agentLog = data -> called[0] = true;
        agentLog.addLog(logData);
        assertTrue(called[0]);
    }
}
