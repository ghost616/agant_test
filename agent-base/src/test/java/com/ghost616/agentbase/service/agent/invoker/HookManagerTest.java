package com.ghost616.agentbase.service.agent.invoker;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.AgentExecutionContext;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class HookManagerTest {

    private HookManager hookManager;

    @Mock
    private AgentExecutionContext ctx;
    @Mock
    private HookData data;
    @Mock
    private HookInvoker regularHook1;
    @Mock
    private HookInvoker regularHook2;
    @Mock
    private SystemHook systemHook1;
    @Mock
    private SystemHook systemHook2;
    @Mock
    private SystemPostHook postHook1;
    @Mock
    private SystemPostHook postHook2;
    @Mock
    private AgentComponentRegistry registry;
    @Mock
    private ChatDataProvider chatDataProvider;

    @BeforeEach
    void setUp() {
        hookManager = new HookManager(registry);
        when(registry.getChatDataProvider()).thenReturn(chatDataProvider);
        when(regularHook1.getPhase()).thenReturn(HookPhase.BEFORE_MESSAGE_SEND);
        when(regularHook2.getPhase()).thenReturn(HookPhase.BEFORE_MESSAGE_SEND);
        when(systemHook1.getPhase()).thenReturn(HookPhase.BEFORE_MESSAGE_SEND);
        when(systemHook2.getPhase()).thenReturn(HookPhase.BEFORE_MESSAGE_SEND);
        when(systemHook1.getIndex()).thenReturn(10);
        when(systemHook2.getIndex()).thenReturn(5);
        when(postHook1.getIndex()).thenReturn(2);
        when(postHook2.getIndex()).thenReturn(1);
    }

    // ==================== 正向覆盖 ====================

    @Test
    void regularHooks执行成功() {
        when(chatDataProvider.getHooks()).thenReturn(List.of(regularHook1, regularHook2));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        verify(regularHook1).execute(ctx, data);
        verify(regularHook2).execute(ctx, data);
    }

    @Test
    void systemHooks按index升序执行() {
        when(chatDataProvider.getHooks()).thenReturn(List.of(systemHook1, systemHook2));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        verify(systemHook2).execute(ctx, data);
        verify(systemHook1).execute(ctx, data);
    }

    @Test
    void systemPostHooks按index升序执行() {
        when(chatDataProvider.getHooks()).thenReturn(List.of(postHook1, postHook2));
        hookManager.refreshHooks();
        hookManager.executePostHooks(ctx, data);
        verify(postHook2).execute(ctx, data);
        verify(postHook1).execute(ctx, data);
    }

    @Test
    void 所有类型hooks同时执行() {
        when(chatDataProvider.getHooks()).thenReturn(List.of(regularHook1, systemHook1, postHook1));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        hookManager.executePostHooks(ctx, data);
        verify(regularHook1).execute(ctx, data);
        verify(systemHook1).execute(ctx, data);
        verify(postHook1).execute(ctx, data);
    }

    // ==================== 反向覆盖 ====================

    @Test
    void regularHook抛出异常不影响后续hook执行() {
        doThrow(new RuntimeException("hook1 fail")).when(regularHook1).execute(ctx, data);
        when(chatDataProvider.getHooks()).thenReturn(List.of(regularHook1, regularHook2));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        verify(regularHook1).execute(ctx, data);
        verify(regularHook2).execute(ctx, data);
    }

    @Test
    void systemHook抛出异常不影响后续hook执行() {
        doThrow(new RuntimeException("systemHook1 fail")).when(systemHook1).execute(ctx, data);
        when(chatDataProvider.getHooks()).thenReturn(List.of(systemHook1, systemHook2));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        verify(systemHook1).execute(ctx, data);
        verify(systemHook2).execute(ctx, data);
    }

    @Test
    void postHook抛出异常不影响后续hook执行() {
        doThrow(new RuntimeException("postHook1 fail")).when(postHook1).execute(ctx, data);
        when(chatDataProvider.getHooks()).thenReturn(List.of(postHook1, postHook2));
        hookManager.refreshHooks();
        hookManager.executePostHooks(ctx, data);
        verify(postHook1).execute(ctx, data);
        verify(postHook2).execute(ctx, data);
    }

    @Test
    void 所有hook同时抛出异常不中断整体流程() {
        doThrow(new RuntimeException("fail")).when(regularHook1).execute(ctx, data);
        doThrow(new RuntimeException("fail")).when(systemHook1).execute(ctx, data);
        doThrow(new RuntimeException("fail")).when(postHook1).execute(ctx, data);
        when(chatDataProvider.getHooks()).thenReturn(List.of(regularHook1, systemHook1, postHook1));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        hookManager.executePostHooks(ctx, data);
        verify(regularHook1).execute(ctx, data);
        verify(systemHook1).execute(ctx, data);
        verify(postHook1).execute(ctx, data);
    }

    // ==================== 边界值 ====================

    @Test
    void triggerHooks无对应phase的hook不执行() {
        when(chatDataProvider.getHooks()).thenReturn(List.of(regularHook1));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.SESSION_START, ctx, data);
        verify(regularHook1, never()).execute(any(), any());
    }

    @Test
    void refreshHooks空列表所有方法不抛异常() {
        when(chatDataProvider.getHooks()).thenReturn(List.of());
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        hookManager.executePostHooks(ctx, data);
    }

    @Test
    void 只有systemHooks时regularHooks不执行() {
        when(chatDataProvider.getHooks()).thenReturn(List.of(systemHook1));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        verify(systemHook1).execute(ctx, data);
    }

    @Test
    void 只有regularHooks时systemHooks不执行() {
        when(chatDataProvider.getHooks()).thenReturn(List.of(regularHook1));
        hookManager.refreshHooks();
        hookManager.triggerHooks(HookPhase.BEFORE_MESSAGE_SEND, ctx, data);
        verify(regularHook1).execute(ctx, data);
        verifyNoMoreInteractions(systemHook1, systemHook2, postHook1, postHook2);
    }
}
