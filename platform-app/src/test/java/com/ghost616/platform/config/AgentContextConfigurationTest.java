package com.ghost616.platform.config;

import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.agentbase.service.agent.invoker.SystemToolProvider;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import com.ghost616.agentinteg.AgentAssembler;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.agent.DefaultChatDataProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

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

        AgentAssembler agentAssembler = config.agentAssembler(systemToolProvider, modelInvokerFactory, chatDataProvider);

        assertNotNull(agentAssembler);
    }

    @Test
    void chatService_通过AgentAssembler创建() {
        SystemToolProvider systemToolProvider = mock(SystemToolProvider.class);
        ModelInvokerFactory modelInvokerFactory = mock(ModelInvokerFactory.class);
        ChatDataProvider chatDataProvider = mock(ChatDataProvider.class);

        AgentAssembler agentAssembler = config.agentAssembler(systemToolProvider, modelInvokerFactory, chatDataProvider);
        ChatService chatService = config.chatService(agentAssembler);

        assertNotNull(chatService);
    }

    @Test
    void toolExecutionService_通过AgentAssembler创建() {
        SystemToolProvider systemToolProvider = mock(SystemToolProvider.class);
        ModelInvokerFactory modelInvokerFactory = mock(ModelInvokerFactory.class);
        ChatDataProvider chatDataProvider = mock(ChatDataProvider.class);

        AgentAssembler agentAssembler = config.agentAssembler(systemToolProvider, modelInvokerFactory, chatDataProvider);
        ToolExecutionService toolExecutionService = config.toolExecutionService(agentAssembler);

        assertNotNull(toolExecutionService);
    }
}
