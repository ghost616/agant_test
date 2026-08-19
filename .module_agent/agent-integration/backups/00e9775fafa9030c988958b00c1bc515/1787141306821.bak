package com.ghost616.agentinteg;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.core.ThreadVariableHandler;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.agentbase.service.agent.invoker.HookManager;
import com.ghost616.agentbase.service.agent.log.AgentLog;
import com.ghost616.agentbase.sendmessage.MessageSender;
import com.ghost616.agentbase.service.agent.ChatDataCacheManager;
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
import java.lang.reflect.Method;
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
        AgentAssembler.Result result = agentAssembler.build();

        // hookManager 通过 ensureInitialized() 懒加载，build() 后未触发初始化前为 null
        Field csField = ChatService.class.getDeclaredField("hookManager");
        csField.setAccessible(true);
        Field tesField = ToolExecutionService.class.getDeclaredField("hookManager");
        tesField.setAccessible(true);
        assertNull(csField.get(result.chatService()));
        assertNull(tesField.get(result.toolExecutionService()));

        // 触发一次初始化（等价于首次调用 chat()/executeTool()）
        invokeEnsureInitialized(result.chatService());
        invokeEnsureInitialized(result.toolExecutionService());

        HookManager csHookManager = (HookManager) csField.get(result.chatService());
        HookManager tesHookManager = (HookManager) tesField.get(result.toolExecutionService());

        assertNotNull(csHookManager);
        assertNotNull(tesHookManager);
        assertSame(csHookManager, tesHookManager);
    }

    @Test
    void refreshHooks_shouldFetchHooksFromChatDataProvider() {
        when(chatDataProvider.getHooks()).thenReturn(List.of());

        agentAssembler.build();
        agentAssembler.refreshHooks();

        verify(chatDataProvider, times(1)).getHooks();
    }

    @Test
    void refreshHooks_shouldPassHooksToHookManagerRefreshHooks() {
        HookInvoker mockHook = mock(HookInvoker.class);
        when(chatDataProvider.getHooks()).thenReturn(List.of(mockHook));

        agentAssembler.build();
        agentAssembler.refreshHooks();

        verify(chatDataProvider, times(1)).getHooks();
    }

    // ========== AgentLog 注册验证 ==========

    @Test
    void setAgentLog_build前调用_build后registry仍为null() throws Exception {
        AgentLog agentLog = mock(AgentLog.class);
        agentAssembler.setAgentLog(agentLog);
        agentAssembler.build();

        assertNull(getRegistry().getAgentLog());
    }

    @Test
    void setAgentLog_build后调用_直接注册到registry() throws Exception {
        AgentLog agentLog = mock(AgentLog.class);
        agentAssembler.build();
        agentAssembler.setAgentLog(agentLog);

        assertSame(agentLog, getRegistry().getAgentLog());
    }

    @Test
    void setAgentLog_build后调用且设置null_registry为null() throws Exception {
        agentAssembler.build();
        agentAssembler.setAgentLog(null);

        assertNull(getRegistry().getAgentLog());
    }

    // ========== ChatDataCacheManager 注册验证 ==========

    @Test
    void setChatDataCacheManager_build前调用_build后registry仍为null() throws Exception {
        ChatDataCacheManager chatDataCacheManager = mock(ChatDataCacheManager.class);
        agentAssembler.setChatDataCacheManager(chatDataCacheManager);
        agentAssembler.build();

        assertNull(getRegistry().getChatDataCacheManager());
    }

    @Test
    void setChatDataCacheManager_build后调用_直接注册到registry() throws Exception {
        ChatDataCacheManager chatDataCacheManager = mock(ChatDataCacheManager.class);
        agentAssembler.build();
        agentAssembler.setChatDataCacheManager(chatDataCacheManager);

        assertSame(chatDataCacheManager, getRegistry().getChatDataCacheManager());
    }

    @Test
    void setChatDataCacheManager_build后调用且设置null_registry为null() throws Exception {
        agentAssembler.build();
        agentAssembler.setChatDataCacheManager(null);

        assertNull(getRegistry().getChatDataCacheManager());
    }

    @Test
    void setChatDataCacheManager_build后调用_透传给agentMessageProxy() throws Exception {
        ChatDataCacheManager chatDataCacheManager = mock(ChatDataCacheManager.class);
        agentAssembler.build();
        agentAssembler.setChatDataCacheManager(chatDataCacheManager);

        Field proxyField = AgentAssembler.class.getDeclaredField("agentMessageProxy");
        proxyField.setAccessible(true);
        Object agentMessageProxy = proxyField.get(agentAssembler);
        assertNotNull(agentMessageProxy);

        Field cacheField = agentMessageProxy.getClass().getDeclaredField("chatDataCacheManager");
        cacheField.setAccessible(true);
        assertSame(chatDataCacheManager, cacheField.get(agentMessageProxy));
    }

    // ========== ThreadVariableHandler 注册验证 ==========

    @Test
    void setThreadVariableHandler_build前调用_build后注册到registry() throws Exception {
        ThreadVariableHandler handler = mock(ThreadVariableHandler.class);
        agentAssembler.setThreadVariableHandler(handler);
        agentAssembler.build();

        assertSame(handler, getRegistry().getThreadVariableHandler());
    }

    @Test
    void setThreadVariableHandler未调用_build后registry为null() throws Exception {
        agentAssembler.build();

        assertNull(getRegistry().getThreadVariableHandler());
    }

    @Test
    void setThreadVariableHandler_build后调用_更新已建registry() throws Exception {
        agentAssembler.build();
        ThreadVariableHandler handler = mock(ThreadVariableHandler.class);
        agentAssembler.setThreadVariableHandler(handler);

        assertSame(handler, getRegistry().getThreadVariableHandler());
    }

    private AgentComponentRegistry getRegistry() throws Exception {
        Field registryField = AgentAssembler.class.getDeclaredField("registry");
        registryField.setAccessible(true);
        return (AgentComponentRegistry) registryField.get(agentAssembler);
    }

    private void invokeEnsureInitialized(Object service) throws Exception {
        Method method = service.getClass().getDeclaredMethod("ensureInitialized");
        method.setAccessible(true);
        method.invoke(service);
    }
}
