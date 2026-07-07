package com.ghost616.platform.config;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.agentbase.service.agent.ToolExecutionTracker;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolProvider;
import com.ghost616.agentbase.service.agent.invoker.HistoryQuerySystemTool;
import com.ghost616.agentbase.service.agent.invoker.LoadSkillsSystemTool;
import com.ghost616.agentbase.service.agent.invoker.MessageSavePostHook;
import com.ghost616.agentbase.service.agent.invoker.ToolCallQueueManager;
import com.ghost616.agentbase.service.agent.invoker.UnloadSkillsSystemTool;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import com.ghost616.platform.repository.ModelConfigMapper;
import com.ghost616.platform.repository.SessionMapper;
import com.ghost616.platform.service.agent.DefaultChatDataProvider;
import com.ghost616.platform.service.model.invoker.DefaultModelInvokerFactory;
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
    public ToolManager toolManager() {
        return new ToolManager(toolDataProvider);
    }

    @Bean
    public ToolCallQueueManager toolCallQueueManager() {
        return new ToolCallQueueManager();
    }

    @Bean
    public SystemToolManager systemToolManager(ApplicationContext applicationContext) {
        SystemToolProvider provider = () -> {
            Map<String, SystemTool> beans = applicationContext.getBeansOfType(SystemTool.class);
            Map<String, SystemTool> tools = new HashMap<>();
            for (SystemTool tool : beans.values()) {
                String toolName = tool.getToolName();
                if (toolName != null && !toolName.isBlank()) {
                    tools.put(toolName, tool);
                }
            }
            return tools;
        };
        return new SystemToolManager(provider);
    }

    @Bean
    public SessionManager sessionManager() {
        return new SessionManager(messageDataProvider);
    }

    @Bean
    public AgentContextManager agentContextManager(SessionManager sessionManager, ToolManager toolManager) {
        return new AgentContextManager(contextDataProvider, sessionManager, toolManager);
    }

    @Bean
    public DefaultModelInvokerFactory defaultModelInvokerFactory(
            RestClient.Builder restClientBuilder,
            WebClient.Builder webClientBuilder) {
        return new DefaultModelInvokerFactory(restClientBuilder, webClientBuilder);
    }

    @Bean
    public ModelInvokerManager modelInvokerManager(ModelInvokerFactory invokerFactory) {
        return new ModelInvokerManager(invokerFactory);
    }

    @Bean
    public HistoryQuerySystemTool historyQuerySystemTool() {
        return new HistoryQuerySystemTool();
    }

    @Bean
    public LoadSkillsSystemTool loadSkillsSystemTool() {
        return new LoadSkillsSystemTool();
    }

    @Bean
    public UnloadSkillsSystemTool unloadSkillsSystemTool() {
        return new UnloadSkillsSystemTool();
    }

    @Bean
    public MessageSavePostHook messageSavePostHook(
            SessionManager sessionManager,
            AgentContextManager agentContextManager,
            ToolCallQueueManager toolCallQueueManager) {
        return new MessageSavePostHook(sessionManager, agentContextManager, toolCallQueueManager);
    }

    @Bean
    public DefaultChatDataProvider defaultChatDataProvider(
            ModelConfigMapper modelConfigMapper,
            SessionMapper sessionMapper,
            ApplicationContext applicationContext) {
        return new DefaultChatDataProvider(modelConfigMapper, sessionMapper, applicationContext);
    }

    @Bean
    public ChatService chatService(
            AgentContextManager agentContextManager,
            SessionManager sessionManager,
            ModelInvokerManager modelInvokerManager,
            SystemToolManager systemToolManager,
            ChatDataProvider chatDataProvider) {
        ChatService chatService = new ChatService(agentContextManager, sessionManager, modelInvokerManager, systemToolManager, chatDataProvider);
        chatService.initHooks();
        return chatService;
    }

    @Bean
    public ToolExecutionTracker toolExecutionTracker() {
        return new ToolExecutionTracker();
    }

    @Bean
    public ToolExecutionService toolExecutionService(
            ToolCallQueueManager toolCallQueueManager,
            ToolManager toolManager,
            SystemToolManager systemToolManager,
            SessionManager sessionManager,
            ChatService chatService,
            AgentContextManager agentContextManager,
            ToolExecutionTracker toolExecutionTracker) {
        return new ToolExecutionService(toolCallQueueManager, toolManager, systemToolManager,
                sessionManager, chatService, agentContextManager, toolExecutionTracker);
    }
}