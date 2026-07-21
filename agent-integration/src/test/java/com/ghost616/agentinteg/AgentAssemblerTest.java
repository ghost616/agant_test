package com.ghost616.agentinteg;

import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.agentbase.service.agent.invoker.HookManager;
import com.ghost616.agentbase.sendmessage.MessageSender;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionProvider;
import com.ghost616.agentbase.service.agent.invoker.SystemToolProvider;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentAssemblerTest {

    @Mock
    private ContextDataProvider contextDataProvider;

    @Mock
    private MessageDataProvider messageDataProvider;

    @Mock
    private ToolDataProvider toolDataProvider;

    @Mock
    private SystemToolProvider systemToolProvider;

    @Mock
    private ModelInvokerFactory modelInvokerFactory;

    @Mock
    private ChatDataProvider chatDataProvider;

    @Mock
    private MessageSender messageSender;

    @Mock
    private ToolExecutionProvider toolExecutionProvider;

    private AgentAssembler agentAssembler;

    @BeforeEach
    void setUp() {
        agentAssembler = new AgentAssembler(contextDataProvider, messageDataProvider, toolDataProvider,
                systemToolProvider, modelInvokerFactory, chatDataProvider, messageSender, toolExecutionProvider);
    }

    @Test
    void build_多次调用返回相同Result实例() {
        AgentAssembler.Result result1 = agentAssembler.build();
        AgentAssembler.Result result2 = agentAssembler.build();

        assertSame(result1, result2);
    }

    @Test
    void build_多次调用返回相同chatService() {
        AgentAssembler.Result result1 = agentAssembler.build();
        AgentAssembler.Result result2 = agentAssembler.build();

        assertSame(result1.chatService(), result2.chatService());
    }

    @Test
    void build_多次调用返回相同toolExecutionService() {
        AgentAssembler.Result result1 = agentAssembler.build();
        AgentAssembler.Result result2 = agentAssembler.build();

        assertSame(result1.toolExecutionService(), result2.toolExecutionService());
    }

    @Test
    void build后sessionManager返回非null() {
        agentAssembler.build();
        assertNotNull(agentAssembler.sessionManager());
    }

    @Test
    void build后toolCallQueueManager返回非null() {
        agentAssembler.build();
        assertNotNull(agentAssembler.toolCallQueueManager());
    }

    @Test
    void build后agentContextManager返回非null() {
        agentAssembler.build();
        assertNotNull(agentAssembler.agentContextManager());
    }

    @Test
    void build前sessionManager返回null() {
        assertNull(agentAssembler.sessionManager());
    }

    @Test
    void build前toolCallQueueManager返回null() {
        assertNull(agentAssembler.toolCallQueueManager());
    }

    @Test
    void build前agentContextManager返回null() {
        assertNull(agentAssembler.agentContextManager());
    }

    // ========== HookManager 共享与调用验证 ==========

    @Test
    void build_shouldShareSameHookManagerInstanceBetweenServices() throws Exception {
        when(chatDataProvider.getHooks()).thenReturn(List.of());

        AgentAssembler.Result result = agentAssembler.build();

        Field csField = ChatService.class.getDeclaredField("hookManager");
        csField.setAccessible(true);
        HookManager csHookManager = (HookManager) csField.get(result.chatService());

        Field tesField = ToolExecutionService.class.getDeclaredField("hookManager");
        tesField.setAccessible(true);
        HookManager tesHookManager = (HookManager) tesField.get(result.toolExecutionService());

        assertNotNull(csHookManager);
        assertNotNull(tesHookManager);
        assertSame(csHookManager, tesHookManager);
    }

    @Test
    void build_shouldCallRefreshHooksOnHookManager() {
        List<HookInvoker> hooks = List.of();
        when(chatDataProvider.getHooks()).thenReturn(hooks);

        agentAssembler.build();

        verify(chatDataProvider, times(1)).getHooks();
    }

    @Test
    void build_shouldPassHooksToHookManagerRefreshHooks() {
        HookInvoker mockHook = mock(HookInvoker.class);
        when(chatDataProvider.getHooks()).thenReturn(List.of(mockHook));

        agentAssembler.build();

        verify(chatDataProvider, times(1)).getHooks();
    }
}
