package com.ghost616.platform.config;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.agent.DefaultChatDataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    void chatService_创建并调用initHooks() {
        AgentContextManager agentContextManager = mock(AgentContextManager.class);
        SessionManager sessionManager = mock(SessionManager.class);
        ModelInvokerManager modelInvokerManager = mock(ModelInvokerManager.class);
        ObjectMapper objectMapper = new ObjectMapper();
        SystemToolManager systemToolManager = mock(SystemToolManager.class);
        ChatDataProvider chatDataProvider = mock(ChatDataProvider.class);

        ChatService chatService = config.chatService(
                agentContextManager, sessionManager, modelInvokerManager,
                objectMapper, systemToolManager, chatDataProvider);

        assertNotNull(chatService);
    }
}
