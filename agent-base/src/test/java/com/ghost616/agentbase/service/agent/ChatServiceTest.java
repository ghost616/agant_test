package com.ghost616.agentbase.service.agent;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.enums.HookPhase;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.agentbase.service.agent.invoker.SystemHook;
import com.ghost616.agentbase.service.agent.invoker.SystemPostHook;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AgentContextManager agentContextManager;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private ModelInvokerManager modelInvokerManager;
    @Mock
    private SystemToolManager systemToolManager;
    @Mock
    private ChatDataProvider chatDataProvider;

    private AgentComponentRegistry registry;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        registry = new AgentComponentRegistry();
        registry.setAgentContextManager(agentContextManager);
        registry.setSessionManager(sessionManager);
        registry.setModelInvokerManager(modelInvokerManager);
        registry.setSystemToolManager(systemToolManager);
        registry.setChatDataProvider(chatDataProvider);
        chatService = new ChatService(registry);
    }

    @Test
    void constructor_shouldInjectAllSixDependencies() {
        assertNotNull(chatService);
    }

    @Test
    void refreshHooks_shouldCallGetHooksOnce() {
        when(chatDataProvider.getHooks()).thenReturn(List.of());
        chatService.refreshHooks();
        verify(chatDataProvider, times(1)).getHooks();
    }

    @Test
    void refreshHooks_shouldClassifySystemPostHookToPostList() throws Exception {
        SystemPostHook postHook = mock(SystemPostHook.class);
        when(postHook.getPhase()).thenReturn(HookPhase.BEFORE_MESSAGE_SEND);
        when(chatDataProvider.getHooks()).thenReturn(List.of(postHook));

        chatService.refreshHooks();

        List<HookInvoker> postHooks = getPrivateField("systemPostHooks");
        assertEquals(1, postHooks.size());
        assertSame(postHook, postHooks.get(0));

        Map<HookPhase, List<HookInvoker>> hooksMap = getPrivateField("systemHooks");
        assertTrue(hooksMap.isEmpty());
    }

    @Test
    void refreshHooks_shouldClassifySystemHookToPhaseMap() throws Exception {
        SystemHook hook = mock(SystemHook.class);
        when(hook.getPhase()).thenReturn(HookPhase.SESSION_START);
        when(chatDataProvider.getHooks()).thenReturn(List.of(hook));

        chatService.refreshHooks();

        Map<HookPhase, List<HookInvoker>> hooksMap = getPrivateField("systemHooks");
        assertEquals(1, hooksMap.size());
        List<HookInvoker> hooks = hooksMap.get(HookPhase.SESSION_START);
        assertNotNull(hooks);
        assertEquals(1, hooks.size());
        assertSame(hook, hooks.get(0));

        List<HookInvoker> postHooks = getPrivateField("systemPostHooks");
        assertTrue(postHooks.isEmpty());
    }

    @Test
    void refreshHooks_shouldGroupSystemHooksByPhase() throws Exception {
        SystemHook hook1 = mock(SystemHook.class);
        when(hook1.getPhase()).thenReturn(HookPhase.SESSION_START);
        SystemHook hook2 = mock(SystemHook.class);
        when(hook2.getPhase()).thenReturn(HookPhase.SESSION_START);
        SystemHook hook3 = mock(SystemHook.class);
        when(hook3.getPhase()).thenReturn(HookPhase.BEFORE_MESSAGE_SEND);

        when(chatDataProvider.getHooks()).thenReturn(List.of(hook1, hook2, hook3));

        chatService.refreshHooks();

        Map<HookPhase, List<HookInvoker>> hooksMap = getPrivateField("systemHooks");
        assertEquals(2, hooksMap.size());
        assertEquals(2, hooksMap.get(HookPhase.SESSION_START).size());
        assertEquals(1, hooksMap.get(HookPhase.BEFORE_MESSAGE_SEND).size());
    }

    @Test
    void refreshHooks_shouldHandleMixedSystemHookAndSystemPostHook() throws Exception {
        SystemHook systemHook = mock(SystemHook.class);
        when(systemHook.getPhase()).thenReturn(HookPhase.SESSION_START);
        SystemPostHook postHook = mock(SystemPostHook.class);
        when(postHook.getPhase()).thenReturn(HookPhase.SESSION_START);
        SystemHook anotherSystemHook = mock(SystemHook.class);
        when(anotherSystemHook.getPhase()).thenReturn(HookPhase.AFTER_MESSAGE_RECEIVE);

        when(chatDataProvider.getHooks()).thenReturn(List.of(systemHook, postHook, anotherSystemHook));

        chatService.refreshHooks();

        Map<HookPhase, List<HookInvoker>> hooksMap = getPrivateField("systemHooks");
        assertEquals(2, hooksMap.size());
        assertEquals(1, hooksMap.get(HookPhase.SESSION_START).size());
        assertSame(systemHook, hooksMap.get(HookPhase.SESSION_START).get(0));
        assertEquals(1, hooksMap.get(HookPhase.AFTER_MESSAGE_RECEIVE).size());
        assertSame(anotherSystemHook, hooksMap.get(HookPhase.AFTER_MESSAGE_RECEIVE).get(0));

        List<HookInvoker> postHooks = getPrivateField("systemPostHooks");
        assertEquals(1, postHooks.size());
        assertSame(postHook, postHooks.get(0));
    }

    @Test
    void refreshHooks_shouldHandleEmptyHookList() throws Exception {
        when(chatDataProvider.getHooks()).thenReturn(List.of());

        chatService.refreshHooks();

        Map<HookPhase, List<HookInvoker>> hooksMap = getPrivateField("systemHooks");
        assertTrue(hooksMap.isEmpty());

        List<HookInvoker> postHooks = getPrivateField("systemPostHooks");
        assertTrue(postHooks.isEmpty());
    }

    @Test
    void refreshHooks_shouldReplaceExistingState() throws Exception {
        SystemHook hook1 = mock(SystemHook.class);
        when(hook1.getPhase()).thenReturn(HookPhase.SESSION_START);
        when(chatDataProvider.getHooks()).thenReturn(List.of(hook1));
        chatService.refreshHooks();

        SystemHook hook2 = mock(SystemHook.class);
        when(hook2.getPhase()).thenReturn(HookPhase.SESSION_START);
        when(chatDataProvider.getHooks()).thenReturn(List.of(hook2));
        chatService.refreshHooks();

        Map<HookPhase, List<HookInvoker>> hooksMap = getPrivateField("systemHooks");
        assertEquals(1, hooksMap.get(HookPhase.SESSION_START).size());
        assertSame(hook2, hooksMap.get(HookPhase.SESSION_START).get(0));
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String fieldName) throws Exception {
        Field field = ChatService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(chatService);
    }
}
