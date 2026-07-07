package com.ghost616.agentbase.service.model.invoker;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelInvokerManager {

    private final ModelInvokerFactory invokerFactory;
    private final Map<Long, ModelInvoker> cache = new ConcurrentHashMap<>();

    public ModelInvokerManager(ModelInvokerFactory invokerFactory) {
        this.invokerFactory = invokerFactory;
    }

    public ModelInvoker getInvoker(ModelConfigData config) {
        return cache.computeIfAbsent(config.id(), id -> invokerFactory.createInvoker(config));
    }

    public void register(Long id, ModelInvoker invoker) {
        cache.put(id, invoker);
    }

    public void evict(Long id) {
        cache.remove(id);
    }

    public void clear() {
        cache.clear();
    }

    public int cacheSize() {
        return cache.size();
    }

    public ModelInvoker getInvokerById(Long id) {
        return cache.get(id);
    }
}
