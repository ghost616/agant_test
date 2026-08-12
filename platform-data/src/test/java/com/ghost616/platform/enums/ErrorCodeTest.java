package com.ghost616.platform.enums;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCodeTest {

    @Test
    void enumCountAndCodes() {
        ErrorCode[] values = ErrorCode.values();
        assertEquals(31, values.length);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("SYSTEM_ERROR", "SYS-001");
        expected.put("PARAM_INVALID", "SYS-002");
        expected.put("MODEL_NOT_FOUND", "MODEL-CONFIG-001");
        expected.put("MODEL_UNSUPPORTED", "MODEL-UNSUPPORTED-001");
        expected.put("MODEL_ALREADY_EXISTS", "MODEL-CONFIG-002");
        expected.put("TOOL_NOT_FOUND", "TOOL-CONFIG-001");
        expected.put("TOOL_ALREADY_EXISTS", "TOOL-CONFIG-002");
        expected.put("TOOL_SCHEMA_INVALID", "TOOL-CONFIG-003");
        expected.put("AGENT_NOT_FOUND", "AGENT-CONFIG-001");
        expected.put("AGENT_ALREADY_EXISTS", "AGENT-CONFIG-002");
        expected.put("AGENT_MEMORY_GROUP_INVALID", "AGENT-CONFIG-003");
        expected.put("AGENT_MEMORY_VECTOR_MODEL_REQUIRED", "AGENT-CONFIG-004");
        expected.put("AGENT_MEMORY_NOT_ENABLED", "AGENT-CONFIG-005");
        expected.put("SKILL_NOT_FOUND", "SKILL-CONFIG-001");
        expected.put("SKILL_ALREADY_EXISTS", "SKILL-CONFIG-002");
        expected.put("SESSION_NOT_FOUND", "SESSION-001");
        expected.put("SESSION_NO_USER_MESSAGE", "SESSION-003");
        expected.put("SUB_SESSION_DATA_NOT_FOUND", "SESSION-004");
        expected.put("CHILD_SESSION_NO_MESSAGES", "SESSION-005");
        expected.put("EVALUATION_NOT_FOUND", "EVAL-001");
        expected.put("EVALUATION_ALREADY_EXISTS", "EVAL-002");
        expected.put("EVALUATION_BENCHMARK_NO_USER_MESSAGE", "EVAL-BENCH-001");
        expected.put("EVALUATION_EXECUTION_STATUS_NOT_FOUND", "EVAL-EXEC-001");
        expected.put("EVALUATION_RESULT_GENERATE_ERROR", "EVAL-EXEC-003");
        expected.put("EVALUATION_RESULT_NOT_FOUND", "EVAL-RES-001");
        expected.put("AGENT_EVALUATION_NOT_FOUND", "AGENT-EVAL-001");
        expected.put("AGENT_EVALUATION_ALREADY_EXISTS", "AGENT-EVAL-002");
        expected.put("KNOWLEDGE_BASE_NOT_FOUND", "KNOWLEDGE-BASE-001");
        expected.put("KNOWLEDGE_BASE_ALREADY_EXISTS", "KNOWLEDGE-BASE-002");
        expected.put("KNOWLEDGE_FILE_NOT_FOUND", "KNOWLEDGE-FILE-001");
        expected.put("KNOWLEDGE_FILE_PUBLISHING", "KNOWLEDGE-FILE-002");
        assertEquals(expected.size(), values.length);
        for (ErrorCode v : values) {
            assertEquals(expected.get(v.name()), v.getCode(), "code mismatch for " + v.name());
        }
    }

    @Test
    void messages() {
        assertEquals("智能体配置不存在", ErrorCode.AGENT_NOT_FOUND.getMessage());
        assertEquals("智能体未开启记忆功能", ErrorCode.AGENT_MEMORY_NOT_ENABLED.getMessage());
        assertTrue(ErrorCode.values()[0].getMessage().length() > 0);
    }
}
