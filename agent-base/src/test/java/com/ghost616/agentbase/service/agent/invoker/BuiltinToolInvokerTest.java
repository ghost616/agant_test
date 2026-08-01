package com.ghost616.agentbase.service.agent.invoker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuiltinToolInvokerTest {

    private final BuiltinToolInvoker invoker = new BuiltinToolInvoker();

    @Test
    void execute_直接返回传入的arguments() {
        String arguments = "{\"key\":\"value\"}";
        assertEquals(arguments, invoker.execute(null, arguments));
    }

    @Test
    void execute_arguments为null时返回null() {
        assertEquals(null, invoker.execute(null, null));
    }

    @Test
    void execute_arguments为空字符串时原样返回() {
        assertEquals("", invoker.execute(null, ""));
    }

    @Test
    void execute_任意arguments均原样透传() {
        assertEquals("abc123", invoker.execute(null, "abc123"));
    }
}
