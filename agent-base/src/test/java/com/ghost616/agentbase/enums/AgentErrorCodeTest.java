package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentErrorCodeTest {

    @Test
    void shouldContainTwelveCodes() {
        assertEquals(12, AgentErrorCode.values().length);
    }

    @Test
    void systemErrorCodeAndMessage() {
        assertEquals("SYS-001", AgentErrorCode.SYSTEM_ERROR.getCode());
        assertEquals("系统内部错误", AgentErrorCode.SYSTEM_ERROR.getMessage());
    }

    @Test
    void paramInvalidCodeAndMessage() {
        assertEquals("SYS-002", AgentErrorCode.PARAM_INVALID.getCode());
        assertEquals("参数校验失败", AgentErrorCode.PARAM_INVALID.getMessage());
    }

    @Test
    void notFoundCodeAndMessage() {
        assertEquals("SYS-003", AgentErrorCode.NOT_FOUND.getCode());
        assertEquals("资源不存在", AgentErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    void duplicateKeyCodeAndMessage() {
        assertEquals("SYS-005", AgentErrorCode.DUPLICATE_KEY.getCode());
        assertEquals("数据重复", AgentErrorCode.DUPLICATE_KEY.getMessage());
    }

    @Test
    void modelCodes() {
        assertEquals("MODEL-CONFIG-001", AgentErrorCode.MODEL_NOT_FOUND.getCode());
        assertEquals("MODEL-INVOKE-001", AgentErrorCode.MODEL_INVOKE_ERROR.getCode());
        assertEquals("MODEL-VERIFY-001", AgentErrorCode.MODEL_VERIFY_ERROR.getCode());
    }

    @Test
    void toolCodes() {
        assertEquals("TOOL-INVOKE-001", AgentErrorCode.TOOL_INVOKE_ERROR.getCode());
        assertEquals("TOOL-RUNTIME-001", AgentErrorCode.TOOL_RUNTIME_NOT_FOUND.getCode());
        assertEquals("TOOL-EXEC-001", AgentErrorCode.TOOL_EXECUTE_TIMEOUT.getCode());
        assertEquals("TOOL-EXEC-002", AgentErrorCode.TOOL_EXECUTE_ERROR.getCode());
    }

    @Test
    void sessionNotFoundCodeAndMessage() {
        assertEquals("SESSION-001", AgentErrorCode.SESSION_NOT_FOUND.getCode());
        assertEquals("会话不存在", AgentErrorCode.SESSION_NOT_FOUND.getMessage());
    }

    @Test
    void allCodesAreUnique() {
        Set<String> codes = Arrays.stream(AgentErrorCode.values())
                .map(AgentErrorCode::getCode)
                .collect(Collectors.toSet());
        assertEquals(AgentErrorCode.values().length, codes.size());
    }

    @Test
    void allMessagesAreNotNull() {
        for (AgentErrorCode errorCode : AgentErrorCode.values()) {
            assertNotNull(errorCode.getMessage());
        }
    }
}
