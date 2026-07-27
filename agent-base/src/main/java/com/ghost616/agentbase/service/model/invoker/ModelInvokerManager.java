package com.ghost616.agentbase.service.model.invoker;

import com.ghost616.agentbase.core.AgentComponentRegistry;
import com.ghost616.agentbase.dto.model.ModelConfigData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelInvokerManager {

    private final AgentComponentRegistry registry;
    private ModelInvokerFactory invokerFactory;
    private final Map<String, ModelInvoker> cache = new ConcurrentHashMap<>();
    private volatile boolean initialized;

    public ModelInvokerManager(AgentComponentRegistry registry) {
        this.registry = registry;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    invokerFactory = registry.getModelInvokerFactory();
                    initialized = true;
                }
            }
        }
    }

    public ModelInvoker getInvoker(ModelConfigData config) {
        ensureInitialized();
        return cache.computeIfAbsent(config.id(), id -> invokerFactory.createInvoker(config));
    }

    public void register(String id, ModelInvoker invoker) {
        cache.put(id, invoker);
    }

    public void evict(String id) {
        cache.remove(id);
    }

    public void clear() {
        cache.clear();
    }

    public int cacheSize() {
        return cache.size();
    }

    public ModelInvoker getInvokerById(String id) {
        return cache.get(id);
    }
}
