package com.ghost616.platform.service.model.invoker;

import com.ghost616.platform.entity.ModelConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;


/**
 * 模型调用器工厂管理器。
 */
@Component
@RequiredArgsConstructor
public class ModelInvokerManager {

    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;

    private final Map<Long, ModelInvoker> cache = new ConcurrentHashMap<>();

    public ModelInvoker getInvoker(ModelConfig config) {
        return cache.computeIfAbsent(config.getId(), id -> createInvoker(config));
    }

    private ModelInvoker createInvoker(ModelConfig config) {
        String apiKey = config.getApiKey();
        String baseUrl = config.getBaseUrl();
        String modelName = config.getModelName();
        Double defaultTemperature = config.getTemperature();
        Integer defaultMaxTokens = config.getMaxTokens();
        return switch (config.getPlatformType()) {
            case OPENAI -> new OpenAIInvoker(apiKey, baseUrl, modelName,
                    defaultTemperature, defaultMaxTokens, restClientBuilder, webClientBuilder);
            case ANTHROPIC -> new AnthropicInvoker(apiKey, baseUrl, modelName,
                    defaultTemperature, defaultMaxTokens, restClientBuilder, webClientBuilder);
            case AZURE -> new AzureInvoker(apiKey, baseUrl, modelName,
                    defaultTemperature, defaultMaxTokens, restClientBuilder, webClientBuilder);
            case OLLAMA -> new OllamaInvoker(apiKey, baseUrl, modelName,
                    defaultTemperature, defaultMaxTokens, restClientBuilder, webClientBuilder);
            case DEEPSEEK -> new DeepSeekInvoker(apiKey, baseUrl, modelName,
                    defaultTemperature, defaultMaxTokens, restClientBuilder, webClientBuilder);
            case CUSTOM -> new CustomInvoker(apiKey, baseUrl, modelName,
                    defaultTemperature, defaultMaxTokens, restClientBuilder, webClientBuilder);
        };
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
