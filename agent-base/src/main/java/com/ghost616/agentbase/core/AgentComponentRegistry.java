package com.ghost616.agentbase.core;

import com.ghost616.agentbase.service.agent.AgentContextManager;
import com.ghost616.agentbase.service.agent.ChatDataProvider;
import com.ghost616.agentbase.service.agent.ContextDataProvider;
import com.ghost616.agentbase.service.agent.MessageDataProvider;
import com.ghost616.agentbase.service.agent.SessionManager;
import com.ghost616.agentbase.service.agent.ToolDataProvider;
import com.ghost616.agentbase.service.agent.ToolExecutionTracker;
import com.ghost616.agentbase.service.agent.invoker.SystemToolManager;
import com.ghost616.agentbase.service.agent.invoker.SystemToolProvider;
import com.ghost616.agentbase.service.agent.invoker.ToolCallQueueManager;
import com.ghost616.agentbase.service.agent.invoker.ToolManager;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerDataProvider;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerManager;
import lombok.Setter;

/**
 * Agent 组件注册表，统一持有 agent-base 模块的各 Provider/Manager/Factory/Tracker 实例。
 * 供 AgentAssembler 增量填充，被 {@code ensureInitialized()} 惰性获取的 Manager/Service 引用。
 */
@Setter
public class AgentComponentRegistry {

    private ContextDataProvider contextDataProvider;
    private MessageDataProvider messageDataProvider;
    private ToolDataProvider toolDataProvider;
    private ChatDataProvider chatDataProvider;
    private ModelInvokerDataProvider modelInvokerDataProvider;
    private SystemToolProvider systemToolProvider;
    private ModelInvokerFactory modelInvokerFactory;

    private ToolManager toolManager;
    private ToolCallQueueManager toolCallQueueManager;
    private SystemToolManager systemToolManager;
    private SessionManager sessionManager;
    private AgentContextManager agentContextManager;
    private ModelInvokerManager modelInvokerManager;

    private ToolExecutionTracker toolExecutionTracker;

    private <T> T requireInitialized(T value, String name) {
        if (value == null) {
            throw new IllegalStateException("AgentComponentRegistry: " + name + " 尚未初始化");
        }
        return value;
    }

    public ContextDataProvider getContextDataProvider() {
        return requireInitialized(contextDataProvider, "contextDataProvider");
    }

    public MessageDataProvider getMessageDataProvider() {
        return requireInitialized(messageDataProvider, "messageDataProvider");
    }

    public ToolDataProvider getToolDataProvider() {
        return requireInitialized(toolDataProvider, "toolDataProvider");
    }

    public ChatDataProvider getChatDataProvider() {
        return requireInitialized(chatDataProvider, "chatDataProvider");
    }

    public ModelInvokerDataProvider getModelInvokerDataProvider() {
        return requireInitialized(modelInvokerDataProvider, "modelInvokerDataProvider");
    }

    public SystemToolProvider getSystemToolProvider() {
        return requireInitialized(systemToolProvider, "systemToolProvider");
    }

    public ModelInvokerFactory getModelInvokerFactory() {
        return requireInitialized(modelInvokerFactory, "modelInvokerFactory");
    }

    public ToolManager getToolManager() {
        return requireInitialized(toolManager, "toolManager");
    }

    public ToolCallQueueManager getToolCallQueueManager() {
        return requireInitialized(toolCallQueueManager, "toolCallQueueManager");
    }

    public SystemToolManager getSystemToolManager() {
        return requireInitialized(systemToolManager, "systemToolManager");
    }

    public SessionManager getSessionManager() {
        return requireInitialized(sessionManager, "sessionManager");
    }

    public AgentContextManager getAgentContextManager() {
        return requireInitialized(agentContextManager, "agentContextManager");
    }

    public ModelInvokerManager getModelInvokerManager() {
        return requireInitialized(modelInvokerManager, "modelInvokerManager");
    }

    public ToolExecutionTracker getToolExecutionTracker() {
        return requireInitialized(toolExecutionTracker, "toolExecutionTracker");
    }
}
