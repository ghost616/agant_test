package com.ghost616.agentbase.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogTypeTest {

    @Test
    void 枚举应可被加载() {
        assertNotNull(LogType.class);
    }

    @Test
    void 枚举值数量应为8() {
        assertEquals(8, LogType.values().length);
    }

    @Test
    void 应包含请求入口与调用来源枚举值() {
        assertNotNull(LogType.REQUEST_ENTRY);
        assertNotNull(LogType.CALL_SOURCE);
        assertNotNull(LogType.ERROR_LOG);
        assertNotNull(LogType.ROUTE);
        assertNotNull(LogType.MODEL_CALL);
        assertNotNull(LogType.STREAM_EVENT);
        assertNotNull(LogType.HISTORY_EXPAND);
        assertNotNull(LogType.SKILL_LOAD);
    }

    @Test
    void REQUEST_ENTRY的code与description应正确() {
        assertEquals("REQUEST_ENTRY", LogType.REQUEST_ENTRY.getCode());
        assertEquals("请求入口", LogType.REQUEST_ENTRY.getDescription());
    }

    @Test
    void CALL_SOURCE的code与description应正确() {
        assertEquals("CALL_SOURCE", LogType.CALL_SOURCE.getCode());
        assertEquals("调用来源", LogType.CALL_SOURCE.getDescription());
    }

    @Test
    void ERROR_LOG的code与description应正确() {
        assertEquals("ERROR_LOG", LogType.ERROR_LOG.getCode());
        assertEquals("错误日志", LogType.ERROR_LOG.getDescription());
    }

    @Test
    void ROUTE的code与description应正确() {
        assertEquals("ROUTE", LogType.ROUTE.getCode());
        assertEquals("路由分发", LogType.ROUTE.getDescription());
    }

    @Test
    void MODEL_CALL的code与description应正确() {
        assertEquals("MODEL_CALL", LogType.MODEL_CALL.getCode());
        assertEquals("模型调用", LogType.MODEL_CALL.getDescription());
    }

    @Test
    void STREAM_EVENT的code与description应正确() {
        assertEquals("STREAM_EVENT", LogType.STREAM_EVENT.getCode());
        assertEquals("流式事件", LogType.STREAM_EVENT.getDescription());
    }

    @Test
    void HISTORY_EXPAND的code与description应正确() {
        assertEquals("HISTORY_EXPAND", LogType.HISTORY_EXPAND.getCode());
        assertEquals("历史展开", LogType.HISTORY_EXPAND.getDescription());
    }

    @Test
    void SKILL_LOAD的code与description应正确() {
        assertEquals("SKILL_LOAD", LogType.SKILL_LOAD.getCode());
        assertEquals("技能加载", LogType.SKILL_LOAD.getDescription());
    }
}
