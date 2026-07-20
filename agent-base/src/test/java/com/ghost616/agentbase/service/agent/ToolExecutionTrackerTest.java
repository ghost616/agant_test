package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolExecutionTrackerTest {

    @Mock
    private ToolExecutionProvider provider;

    private AgentComponentRegistry registry;
    private ToolExecutionTracker tracker;

    private final Long sessionId = 1L;
    private final String toolId = "tool1";
    private final String toolName = "TestTool";
    private final String arguments = "args";
    private final String result = "done";
    private final String error = "error msg";

    @BeforeEach
    void setUp() {
        registry = new AgentComponentRegistry();
        registry.setToolExecutionProvider(provider);
        tracker = new ToolExecutionTracker(registry);
    }

    @Test
    void setExecuting_shouldDelegateToProvider() {
        tracker.setExecuting(sessionId, toolId, toolName, arguments, false);
        verify(provider).updateExecution(sessionId, toolId, toolName, arguments, "executing", null, false);
    }

    @Test
    void setExecuting_withHasMore_shouldPassHasMore() {
        tracker.setExecuting(sessionId, toolId, toolName, arguments, true);
        verify(provider).updateExecution(sessionId, toolId, toolName, arguments, "executing", null, true);
    }

    @Test
    void setDone_shouldDelegateToProvider() {
        var status = new ToolExecutionTracker.ToolExecutionStatus(toolId, toolName, arguments, "executing", null, false);
        when(provider.getCurrentExecution(sessionId, toolId)).thenReturn(status);

        tracker.setDone(sessionId, toolId, result);

        verify(provider).getCurrentExecution(sessionId, toolId);
        verify(provider).updateExecution(sessionId, toolId, toolName, arguments, "done", result, false);
    }

    @Test
    void setDone_whenCurrentIsNull_shouldNotUpdate() {
        when(provider.getCurrentExecution(sessionId, toolId)).thenReturn(null);

        tracker.setDone(sessionId, toolId, result);

        verify(provider).getCurrentExecution(sessionId, toolId);
        verify(provider, never()).updateExecution(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void setFailed_shouldDelegateToProvider() {
        var status = new ToolExecutionTracker.ToolExecutionStatus(toolId, toolName, arguments, "executing", null, true);
        when(provider.getCurrentExecution(sessionId, toolId)).thenReturn(status);

        tracker.setFailed(sessionId, toolId, error);

        verify(provider).getCurrentExecution(sessionId, toolId);
        verify(provider).updateExecution(sessionId, toolId, toolName, arguments, "failed", error, true);
    }

    @Test
    void setFailed_whenCurrentIsNull_shouldNotUpdate() {
        when(provider.getCurrentExecution(sessionId, toolId)).thenReturn(null);

        tracker.setFailed(sessionId, toolId, error);

        verify(provider, never()).updateExecution(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void clear_shouldDelegateToProvider() {
        tracker.clear(sessionId);
        verify(provider).clearTracking(sessionId);
    }

    @Test
    void getCurrentExecution_shouldDelegateToProvider() {
        var expected = new ToolExecutionTracker.ToolExecutionStatus(toolId, toolName, arguments, "executing", null, false);
        when(provider.getCurrentExecution(sessionId, toolId)).thenReturn(expected);

        var actual = tracker.getCurrentExecution(sessionId, toolId);

        assertSame(expected, actual);
        verify(provider).getCurrentExecution(sessionId, toolId);
    }

    @Test
    void getAndClearResults_shouldDelegateToProvider() {
        var results = List.of(
                new ToolExecutionTracker.ToolResult(toolId, toolName, arguments, result)
        );
        when(provider.getAndClearResults(sessionId)).thenReturn(results);

        var actual = tracker.getAndClearResults(sessionId);

        assertSame(results, actual);
        verify(provider).getAndClearResults(sessionId);
    }

    @Test
    void setDone_withHasMore_shouldPreserveHasMore() {
        var status = new ToolExecutionTracker.ToolExecutionStatus(toolId, toolName, arguments, "executing", null, true);
        when(provider.getCurrentExecution(sessionId, toolId)).thenReturn(status);

        tracker.setDone(sessionId, toolId, result);

        verify(provider).updateExecution(sessionId, toolId, toolName, arguments, "done", result, true);
    }
}
