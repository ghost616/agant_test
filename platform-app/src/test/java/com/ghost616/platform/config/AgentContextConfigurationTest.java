package com.ghost616.platform.config;

import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.agentbase.service.agent.ToolExecutionProvider;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvokerProvider;
import com.ghost616.agentbase.service.agent.invoker.SystemToolProvider;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import com.ghost616.agentinteg.AgentAssembler;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.agent.DefaultChatDataProvider;
import com.ghost616.platform.service.agent.DefaultSubSessionCallback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentContextConfigurationTest {

    private final AgentContextConfiguration config = new AgentContextConfiguration(
            mock(ContextDataProvider.class),
            mock(MessageDataProvider.class),
            mock(ToolDataProvider.class)
    );

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @Mock
    private SessionMapper sessionMapper;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private DefaultSubSessionCallback defaultSubSessionCallback;

    @Mock
    private ToolExecutionProvider toolExecutionProvider;

    @Mock
    private CustomToolInvokerProvider customToolInvokerProvider;

    @Test
    void defaultChatDataProvider_正确创建实例() {
        DefaultChatDataProvider provider = config.defaultChatDataProvider(
                modelConfigMapper, sessionMapper, applicationContext);

        assertNotNull(provider);
    }

    @Test
    void agentAssembler_正确创建实例() {
        SystemToolProvider systemToolProvider = mock(SystemToolProvider.class);
        ModelInvokerFactory modelInvokerFactory = mock(ModelInvokerFactory.class);
        ChatDataProvider chatDataProvider = mock(ChatDataProvider.class);

        AgentAssembler agentAssembler = config.agentAssembler(systemToolProvider, modelInvokerFactory, chatDataProvider, toolExecutionProvider, customToolInvokerProvider);

        assertNotNull(agentAssembler);
    }

    @Test
    void chatService_通过AgentAssembler创建() {
        SystemToolProvider systemToolProvider = mock(SystemToolProvider.class);
        ModelInvokerFactory modelInvokerFactory = mock(ModelInvokerFactory.class);
        ChatDataProvider chatDataProvider = mock(ChatDataProvider.class);

        AgentAssembler agentAssembler = config.agentAssembler(systemToolProvider, modelInvokerFactory, chatDataProvider, toolExecutionProvider, customToolInvokerProvider);
        ChatService chatService = config.chatService(agentAssembler);

        assertNotNull(chatService);
    }

    @Test
    void toolExecutionService_通过AgentAssembler创建() {
        SystemToolProvider systemToolProvider = mock(SystemToolProvider.class);
        ModelInvokerFactory modelInvokerFactory = mock(ModelInvokerFactory.class);
        ChatDataProvider chatDataProvider = mock(ChatDataProvider.class);

        AgentAssembler agentAssembler = config.agentAssembler(systemToolProvider, modelInvokerFactory, chatDataProvider, toolExecutionProvider, customToolInvokerProvider);
        ToolExecutionService toolExecutionService = config.toolExecutionService(agentAssembler);

        assertNotNull(toolExecutionService);
    }

    @Test
    void systemToolProvider返回的工具Map包含callback_sub_session() {
        when(applicationContext.getBeansOfType(SystemTool.class)).thenReturn(Map.of());

        var provider = config.systemToolProvider(applicationContext, defaultSubSessionCallback);
        Map<String, SystemTool> tools = provider.discoverSystemTools();

        assertTrue(tools.containsKey("callback_sub_session"));
        assertNotNull(tools.get("callback_sub_session"));
    }

    @Test
    void systemToolProvider包含ApplicationContext中注册的SystemTool() {
        SystemTool mockTool = mock(SystemTool.class);
        when(mockTool.getToolName()).thenReturn("my_custom_tool");
        when(applicationContext.getBeansOfType(SystemTool.class))
                .thenReturn(Map.of("myCustomTool", mockTool));

        var provider = config.systemToolProvider(applicationContext, defaultSubSessionCallback);
        Map<String, SystemTool> tools = provider.discoverSystemTools();

        assertTrue(tools.containsKey("my_custom_tool"));
        assertTrue(tools.containsKey("callback_sub_session"));
        assertEquals(2, tools.size());
    }

    @Test
    void systemToolProvider过滤掉toolName为null的SystemTool() {
        SystemTool nullNameTool = mock(SystemTool.class);
        when(nullNameTool.getToolName()).thenReturn(null);
        when(applicationContext.getBeansOfType(SystemTool.class))
                .thenReturn(Map.of("nullNameTool", nullNameTool));

        var provider = config.systemToolProvider(applicationContext, defaultSubSessionCallback);
        Map<String, SystemTool> tools = provider.discoverSystemTools();

        assertFalse(tools.containsKey(null));
        assertTrue(tools.containsKey("callback_sub_session"));
        assertEquals(1, tools.size());
    }

    @Test
    void systemToolProvider过滤掉toolName为空字符串的SystemTool() {
        SystemTool blankNameTool = mock(SystemTool.class);
        when(blankNameTool.getToolName()).thenReturn("");
        when(applicationContext.getBeansOfType(SystemTool.class))
                .thenReturn(Map.of("blankNameTool", blankNameTool));

        var provider = config.systemToolProvider(applicationContext, defaultSubSessionCallback);
        Map<String, SystemTool> tools = provider.discoverSystemTools();

        assertTrue(tools.containsKey("callback_sub_session"));
        assertEquals(1, tools.size());
    }
}
