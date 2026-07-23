package com.ghost616.platform.config;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.agentbase.service.agent.invoker.CustomToolInvokerProvider;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentbase.service.agent.invoker.SystemToolProvider;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.agentbase.service.agent.ToolExecutionProvider;
import com.ghost616.agentbase.sendmessage.MessageSender;
import com.ghost616.agentinteg.AgentAssembler;
import com.ghost616.agentinteg.model.invoker.DefaultModelInvokerFactory;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.agent.DefaultChatDataProvider;
import com.ghost616.platform.service.agent.DefaultCustomToolInvokerProvider;
import com.ghost616.platform.service.agent.DefaultToolExecutionProvider;
import com.ghost616.platform.service.agent.DefaultSubSessionCallback;
import com.ghost616.agentinteg.tool.SubSessionCallbackSystemTool;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AgentContextConfiguration {

    private final ContextDataProvider contextDataProvider;
    private final MessageDataProvider messageDataProvider;
    private final ToolDataProvider toolDataProvider;

    @Bean
    public SystemToolProvider systemToolProvider(ApplicationContext applicationContext,
                                                  DefaultSubSessionCallback defaultSubSessionCallback) {
        return () -> {
            Map<String, SystemTool> beans = applicationContext.getBeansOfType(SystemTool.class);
            Map<String, SystemTool> tools = new HashMap<>();
            for (SystemTool tool : beans.values()) {
                String toolName = tool.getToolName();
                if (toolName != null && !toolName.isBlank()) {
                    tools.put(toolName, tool);
                }
            }
            tools.put("callback_sub_session", new SubSessionCallbackSystemTool(defaultSubSessionCallback));
            return tools;
        };
    }

    @Bean
    public DefaultChatDataProvider defaultChatDataProvider(
            ModelConfigMapper modelConfigMapper,
            SessionMapper sessionMapper,
            ApplicationContext applicationContext) {
        return new DefaultChatDataProvider(modelConfigMapper, sessionMapper, applicationContext);
    }

    @Bean
    public ModelInvokerFactory modelInvokerFactory(
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {
        return new DefaultModelInvokerFactory(restClientBuilder, webClientBuilder);
    }

    @Bean
    public DefaultToolExecutionProvider toolExecutionProvider() {
        return new DefaultToolExecutionProvider();
    }

    @Bean
    public DefaultCustomToolInvokerProvider defaultCustomToolInvokerProvider() {
        return new DefaultCustomToolInvokerProvider();
    }

    @Bean
    public AgentAssembler agentAssembler(SystemToolProvider systemToolProvider,
                            ModelInvokerFactory modelInvokerFactory,
                            ChatDataProvider chatDataProvider,
                            ToolExecutionProvider toolExecutionProvider,
                            CustomToolInvokerProvider customToolInvokerProvider) {
        return new AgentAssembler(contextDataProvider, messageDataProvider, toolDataProvider,
                systemToolProvider, modelInvokerFactory, chatDataProvider, null, toolExecutionProvider, customToolInvokerProvider);
    }

    @Bean
    public SessionManager sessionManager(AgentAssembler agentAssembler) {
        agentAssembler.build();
        return agentAssembler.sessionManager();
    }

    @Bean
    public AgentContextManager agentContextManager(AgentAssembler agentAssembler) {
        agentAssembler.build();
        return agentAssembler.agentContextManager();
    }

    @Bean
    public ToolManager toolManager(AgentAssembler agentAssembler) {
        agentAssembler.build();
        return agentAssembler.toolManager();
    }

    @Bean
    public ModelInvokerManager modelInvokerManager(AgentAssembler agentAssembler) {
        agentAssembler.build();
        return agentAssembler.modelInvokerManager();
    }

    @Bean
    public ChatService chatService(AgentAssembler agentAssembler) {
        ChatService chatService = agentAssembler.build().chatService();
        agentAssembler.refreshHooks();
        return chatService;
    }

    @Bean
    public ToolExecutionService toolExecutionService(AgentAssembler agentAssembler) {
        return agentAssembler.build().toolExecutionService();
    }
}