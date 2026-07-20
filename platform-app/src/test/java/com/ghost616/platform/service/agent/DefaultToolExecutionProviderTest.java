package com.ghost616.platform.service.agent;

import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultToolExecutionProviderTest {

    private DefaultToolExecutionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultToolExecutionProvider();
    }

    // ==================== enqueue ====================

    @Test
    void enqueue_null_不操作() {
        provider.enqueue(1L, null);
        assertFalse(provider.hasPending(1L));
    }

    @Test
    void enqueue_empty_不操作() {
        provider.enqueue(1L, List.of());
        assertFalse(provider.hasPending(1L));
    }

    @Test
    void enqueue_正常入队_队列有数据() {
        provider.enqueue(1L, List.of(
                new MessageDataProvider.ToolCallData("tc1", "tool1", "{}"),
                new MessageDataProvider.ToolCallData("tc2", "tool2", "{\"a\":1}")
        ));
        assertTrue(provider.hasPending(1L));
    }

    // ==================== poll ====================

    @Test
    void poll_空队列_返回null() {
        assertNull(provider.poll(1L));
    }

    @Test
    void poll_不存在session_返回null() {
        assertNull(provider.poll(999L));
    }

    @Test
    void poll_有元素_返回并移除队首() {
        MessageDataProvider.ToolCallData first = new MessageDataProvider.ToolCallData("tc1", "tool1", "{}");
        MessageDataProvider.ToolCallData second = new MessageDataProvider.ToolCallData("tc2", "tool2", "{\"a\":1}");
        provider.enqueue(1L, List.of(first, second));

        assertEquals(first, provider.poll(1L));
        assertEquals(second, provider.poll(1L));
        assertNull(provider.poll(1L));
    }

    // ==================== peek ====================

    @Test
    void peek_空队列_返回null() {
        assertNull(provider.peek(1L));
    }

    @Test
    void peek_不存在session_返回null() {
        assertNull(provider.peek(999L));
    }

    @Test
    void peek_有元素_返回不移除() {
        MessageDataProvider.ToolCallData tc = new MessageDataProvider.ToolCallData("tc1", "tool1", "{}");
        provider.enqueue(1L, List.of(tc));

        assertEquals(tc, provider.peek(1L));
        assertTrue(provider.hasPending(1L));
        assertEquals(tc, provider.peek(1L));
    }

    // ==================== hasPending ====================

    @Test
    void hasPending_无队列_返回false() {
        assertFalse(provider.hasPending(1L));
    }

    @Test
    void hasPending_空队列_返回false() {
        provider.enqueue(1L, List.of());
        assertFalse(provider.hasPending(1L));
    }

    @Test
    void hasPending_有元素_返回true() {
        provider.enqueue(1L, List.of(new MessageDataProvider.ToolCallData("tc1", "t", "{}")));
        assertTrue(provider.hasPending(1L));
    }

    // ==================== clearQueue ====================

    @Test
    void clearQueue_清除后队列不存在() {
        provider.enqueue(1L, List.of(new MessageDataProvider.ToolCallData("tc1", "t", "{}")));
        assertTrue(provider.hasPending(1L));

        provider.clearQueue(1L);
        assertFalse(provider.hasPending(1L));
    }

    @Test
    void clearQueue_不影响其他session() {
        provider.enqueue(1L, List.of(new MessageDataProvider.ToolCallData("tc1", "t1", "{}")));
        provider.enqueue(2L, List.of(new MessageDataProvider.ToolCallData("tc2", "t2", "{}")));
        provider.clearQueue(1L);

        assertFalse(provider.hasPending(1L));
        assertTrue(provider.hasPending(2L));
    }

    // ==================== updateExecution ====================

    @Test
    void updateExecution_statusExecuting_创建执行状态和结果列表() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "myTool", "{}", "executing", null, false));

        ToolExecutionTracker.ToolExecutionStatus status = provider.getCurrentExecution(1L, "tool1");
        assertNotNull(status);
        assertEquals("tool1", status.currentToolId());
        assertEquals("myTool", status.currentToolName());
        assertEquals("{}", status.currentArguments());
        assertEquals("executing", status.status());
        assertNull(status.result());
        assertFalse(status.hasMore());
    }

    @Test
    void updateExecution_statusExecuting_hasMore为true() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "myTool", "{}", "executing", null, true));

        ToolExecutionTracker.ToolExecutionStatus status = provider.getCurrentExecution(1L, "tool1");
        assertTrue(status.hasMore());
    }

    @Test
    void updateExecution_statusDone_更新状态并记录结果() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "myTool", "{}", "executing", null, false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", null, null, "done", "success result", false));

        ToolExecutionTracker.ToolExecutionStatus status = provider.getCurrentExecution(1L, "tool1");
        assertEquals("done", status.status());
        assertEquals("success result", status.result());

        List<ToolExecutionTracker.ToolResult> results = provider.getAndClearResults(1L);
        assertEquals(1, results.size());
        assertEquals("success result", results.get(0).result());
    }

    @Test
    void updateExecution_statusFailed_更新状态并记录错误结果() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "myTool", "{}", "executing", null, false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", null, null, "failed", "error msg", false));

        ToolExecutionTracker.ToolExecutionStatus status = provider.getCurrentExecution(1L, "tool1");
        assertEquals("failed", status.status());
        assertEquals("error msg", status.result());

        List<ToolExecutionTracker.ToolResult> results = provider.getAndClearResults(1L);
        assertEquals(1, results.size());
        assertTrue(results.get(0).result().startsWith("[error]"));
        assertTrue(results.get(0).result().contains("error msg"));
    }

    @Test
    void updateExecution_未找到当前状态_不做更新() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("nonexistent", null, null, "done", "result", false));

        assertNull(provider.getCurrentExecution(1L, "nonexistent"));
    }

    @Test
    void updateExecution_未知status_忽略() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "myTool", "{}", "executing", null, false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", null, null, "unknown_status", "result", false));

        ToolExecutionTracker.ToolExecutionStatus status = provider.getCurrentExecution(1L, "tool1");
        assertEquals("executing", status.status());
    }

    // ==================== clearTracking ====================

    @Test
    void clearTracking_按sessionId前缀清除() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t1", "{}", "executing", null, false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool2", "t2", "{}", "executing", null, false));
        provider.updateExecution(2L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t3", "{}", "executing", null, false));

        provider.clearTracking(1L);

        assertNull(provider.getCurrentExecution(1L, "tool1"));
        assertNull(provider.getCurrentExecution(1L, "tool2"));
        assertNotNull(provider.getCurrentExecution(2L, "tool1"));
    }

    @Test
    void clearTracking_不影响其他session的执行和结果() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t1", "{}", "executing", null, false));
        provider.updateExecution(2L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t2", "{}", "executing", null, false));

        provider.clearTracking(1L);

        assertNotNull(provider.getCurrentExecution(2L, "tool1"));
    }

    // ==================== getCurrentExecution ====================

    @Test
    void getCurrentExecution_返回对应key的状态() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "myTool", "{}", "executing", null, false));

        ToolExecutionTracker.ToolExecutionStatus status = provider.getCurrentExecution(1L, "tool1");
        assertNotNull(status);
        assertEquals("myTool", status.currentToolName());
    }

    @Test
    void getCurrentExecution_无记录时返回null() {
        assertNull(provider.getCurrentExecution(1L, "nonexistent"));
    }

    @Test
    void getCurrentExecution_不同session隔离() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t1", "{}", "executing", null, false));
        assertNull(provider.getCurrentExecution(2L, "tool1"));
    }

    // ==================== getAndClearResults ====================

    @Test
    void getAndClearResults_按sessionId前缀收集并清除() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t1", "{}", "executing", null, false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", null, null, "done", "result1", false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool2", "t2", "{}", "executing", null, false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool2", null, null, "done", "result2", false));

        List<ToolExecutionTracker.ToolResult> results = provider.getAndClearResults(1L);

        assertEquals(2, results.size());
        assertNull(provider.getCurrentExecution(1L, "tool1"));
        assertNull(provider.getCurrentExecution(1L, "tool2"));
    }

    @Test
    void getAndClearResults_无结果时返回空列表() {
        List<ToolExecutionTracker.ToolResult> results = provider.getAndClearResults(1L);
        assertTrue(results.isEmpty());
    }

    @Test
    void getAndClearResults_不影响其他session() {
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t1", "{}", "executing", null, false));
        provider.updateExecution(1L, new ToolExecutionTracker.ToolExecutionStatus("tool1", null, null, "done", "r1", false));
        provider.updateExecution(2L, new ToolExecutionTracker.ToolExecutionStatus("tool1", "t2", "{}", "executing", null, false));
        provider.updateExecution(2L, new ToolExecutionTracker.ToolExecutionStatus("tool1", null, null, "done", "r2", false));

        List<ToolExecutionTracker.ToolResult> results = provider.getAndClearResults(1L);

        assertEquals(1, results.size());
        assertNotNull(provider.getCurrentExecution(2L, "tool1"));
        assertNull(provider.getCurrentExecution(1L, "tool1"));
    }
}
