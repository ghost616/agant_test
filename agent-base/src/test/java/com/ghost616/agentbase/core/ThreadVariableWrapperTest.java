package com.ghost616.agentbase.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadVariableWrapperTest {

    @Test
    void clear默认实现为空操作不抛异常() {
        ThreadVariableWrapper wrapper = new ThreadVariableWrapper() {
            @Override
            public void apply() {
                // 测试用空实现，仅验证默认 clear() 行为
            }
        };

        assertDoesNotThrow(wrapper::clear, "默认 clear() 应为空实现且不抛异常");
    }

    @Test
    void clear可被子类覆写生效() {
        AtomicBoolean cleared = new AtomicBoolean(false);
        ThreadVariableWrapper wrapper = new ThreadVariableWrapper() {
            @Override
            public void apply() {
                // 测试用空实现
            }

            @Override
            public void clear() {
                cleared.set(true);
            }
        };

        wrapper.clear();

        assertTrue(cleared.get(), "覆写后的 clear() 应生效");
    }
}