package com.ghost616.agentinteg;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.AgentMessageProxy;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ChatService;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionService;
import com.ghost616.agentbase.service.agent.ToolExecutionTracker;
import com.ghost616.agentbase.service.agent.invoker.HistoryQuerySystemTool;
import com.ghost616.agentbase.service.agent.invoker.HookInvoker;
import com.ghost616.agentbase.service.agent.invoker.HookManager;
import com.ghost616.agentbase.service.agent.invoker.LoadSkillsSystemTool;
import com.ghost616.agentbase.service.agent.invoker.MessageSavePostHook;
import com.ghost616.agentbase.service.agent.invoker.UnloadSkillsSystemTool;
import com.ghost616.agentbase.service.agent.invoker.SessionVariableSystemTool;
import com.ghost616.agentbase.service.agent.invoker.ConversationVariableSystemTool;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.SystemTool;
import com.ghost616.agentbase.service.agent.invoker.SystemToolProvider;
import com.ghost616.agentbase.service.agent.invoker.ToolCallQueueManager;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.sendmessage.MessageSender;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentAssembler {

    private final ContextDataProvider contextDataProvider;
    private final MessageDataProvider messageDataProvider;
    private final ToolDataProvider toolDataProvider;
    private final SystemToolProvider systemToolProvider;
    private final ModelInvokerFactory modelInvokerFactory;
    private final ChatDataProvider chatDataProvider;
    private final MessageSender messageSender;
    private final ToolExecutionProvider toolExecutionProvider;

    private AgentComponentRegistry registry;
    private ChatDataProviderProxy chatDataProviderProxy;
    private Result cachedResult;
    private HookManager hookManager;
    private boolean built = false;

    public AgentAssembler(ContextDataProvider contextDataProvider,
                 MessageDataProvider messageDataProvider,
                 ToolDataProvider toolDataProvider,
                 SystemToolProvider systemToolProvider,
                 ModelInvokerFactory modelInvokerFactory,
                 ChatDataProvider chatDataProvider,
                 MessageSender messageSender,
                 ToolExecutionProvider toolExecutionProvider) {
        this.contextDataProvider = contextDataProvider;
        this.messageDataProvider = messageDataProvider;
        this.toolDataProvider = toolDataProvider;
        this.systemToolProvider = systemToolProvider;
        this.modelInvokerFactory = modelInvokerFactory;
        this.chatDataProvider = chatDataProvider;
        this.messageSender = messageSender;
        this.toolExecutionProvider = toolExecutionProvider;
    }

    public ToolManager toolManager() { return registry != null ? registry.getToolManager() : null; }
    public ToolCallQueueManager toolCallQueueManager() { return registry != null ? registry.getToolCallQueueManager() : null; }
    public SystemToolManager systemToolManager() { return registry != null ? registry.getSystemToolManager() : null; }
    public SessionManager sessionManager() { return registry != null ? registry.getSessionManager() : null; }
    public AgentContextManager agentContextManager() { return registry != null ? registry.getAgentContextManager() : null; }
    public ModelInvokerManager modelInvokerManager() { return registry != null ? registry.getModelInvokerManager() : null; }
    public ToolExecutionTracker toolExecutionTracker() { return registry != null ? registry.getToolExecutionTracker() : null; }
    public MessageSavePostHook messageSavePostHook() { return chatDataProviderProxy != null ? chatDataProviderProxy.getMessageSavePostHook() : null; }

    public void refreshHooks() {
        if (hookManager != null) {
            hookManager.refreshHooks();
        }
    }

    public Result build() {
        if (built) {
            return cachedResult;
        }
        registry = new AgentComponentRegistry();
        registry.setContextDataProvider(contextDataProvider);
        registry.setMessageDataProvider(messageDataProvider);
        registry.setToolDataProvider(toolDataProvider);
        registry.setModelInvokerFactory(modelInvokerFactory);
        registry.setMessageSender(messageSender);
        registry.setToolExecutionProvider(toolExecutionProvider);

        SystemToolProvider systemToolProviderProxy = new SystemToolProviderProxy(systemToolProvider);
        registry.setSystemToolProvider(systemToolProviderProxy);

        registry.setToolManager(new ToolManager(registry));
        registry.setToolCallQueueManager(new ToolCallQueueManager(registry));
        registry.setSystemToolManager(new SystemToolManager(registry));
        registry.setSessionManager(new SessionManager(registry));
        registry.setAgentContextManager(new AgentContextManager(registry));
        registry.setModelInvokerManager(new ModelInvokerManager(registry));
        registry.setToolExecutionTracker(new ToolExecutionTracker(registry));

        chatDataProviderProxy = new ChatDataProviderProxy(chatDataProvider, registry);
        registry.setChatDataProvider(chatDataProviderProxy);

        ChatService chatService = new ChatService(registry);

        ToolExecutionService toolExecutionService = new ToolExecutionService(registry, chatService);

        this.hookManager = new HookManager(registry);
        registry.setHookManager(this.hookManager);

        AgentMessageProxy agentMessageProxy = new AgentMessageProxy(chatService, toolExecutionService);
        registry.getAgentContextManager().setAgentMessageProxy(agentMessageProxy);

        cachedResult = new Result(chatService, toolExecutionService, chatDataProviderProxy.getMessageSavePostHook());
        built = true;
        return cachedResult;
    }

    public record Result(ChatService chatService, ToolExecutionService toolExecutionService, MessageSavePostHook messageSavePostHook) {}

    private static class SystemToolProviderProxy implements SystemToolProvider {
        private final SystemToolProvider delegate;

        SystemToolProviderProxy(SystemToolProvider delegate) {
            this.delegate = delegate;
        }

        public Map<String, SystemTool> discoverSystemTools() {
            Map<String, SystemTool> tools = delegate.discoverSystemTools();
            Map<String, SystemTool> result = (tools != null) ? new HashMap<>(tools) : new HashMap<>();
            result.putIfAbsent("history_query", new HistoryQuerySystemTool());
            result.putIfAbsent("load_skills", new LoadSkillsSystemTool());
            result.putIfAbsent("unload_skills", new UnloadSkillsSystemTool());
            result.putIfAbsent("session_variable", new SessionVariableSystemTool());
            result.putIfAbsent("conversation_variable", new ConversationVariableSystemTool());
            return result;
        }
    }

    private static class ChatDataProviderProxy implements ChatDataProvider {
        private final ChatDataProvider delegate;
        private final AgentComponentRegistry registry;
        private volatile MessageSavePostHook messageSavePostHook;

        ChatDataProviderProxy(ChatDataProvider delegate, AgentComponentRegistry registry) {
            this.delegate = delegate;
            this.registry = registry;
        }

        public ModelConfigData getModelConfig(Long modelId) {
            return delegate.getModelConfig(modelId);
        }

        public void updateSessionModelId(Long sessionId, Long modelId) {
            delegate.updateSessionModelId(sessionId, modelId);
        }

        public List<HookInvoker> getHooks() {
            List<HookInvoker> hooks = delegate.getHooks();
            boolean hasMessageSavePostHook = hooks.stream().anyMatch(h -> h instanceof MessageSavePostHook);
            if (!hasMessageSavePostHook) {
                MessageSavePostHook hook = getOrCreateMessageSavePostHook();
                hooks = new ArrayList<>(hooks);
                hooks.add(hook);
            }
            return hooks;
        }

        public List<HookInvoker> getHooks(Long sessionId) {
            return delegate.getHooks(sessionId);
        }

        MessageSavePostHook getMessageSavePostHook() {
            return getOrCreateMessageSavePostHook();
        }

        private MessageSavePostHook getOrCreateMessageSavePostHook() {
            if (messageSavePostHook == null) {
                synchronized (this) {
                    if (messageSavePostHook == null) {
                        messageSavePostHook = new MessageSavePostHook(
                                registry.getSessionManager(), registry.getAgentContextManager(), registry.getToolCallQueueManager());
                    }
                }
            }
            return messageSavePostHook;
        }
    }
}
