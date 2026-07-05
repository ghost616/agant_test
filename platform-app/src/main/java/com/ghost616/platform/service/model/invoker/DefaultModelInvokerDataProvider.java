package com.ghost616.platform.service.model.invoker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerDataProvider;


/**
 * 默认模型调用器数据提供者实现，根据平台类型创建对应的 Invoker 实例。
 */
@Component
@RequiredArgsConstructor
public class DefaultModelInvokerDataProvider implements ModelInvokerDataProvider {

    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;

    @Override
    public ModelInvoker createInvoker(String platformType, String apiKey, String baseUrl,
                                      String modelName, Double temperature, Integer maxTokens) {
        return switch (platformType) {
            case "OPENAI" -> new OpenAIInvoker(apiKey, baseUrl, modelName,
                    temperature, maxTokens, restClientBuilder, webClientBuilder);
            case "ANTHROPIC" -> new AnthropicInvoker(apiKey, baseUrl, modelName,
                    temperature, maxTokens, restClientBuilder, webClientBuilder);
            case "AZURE" -> new AzureInvoker(apiKey, baseUrl, modelName,
                    temperature, maxTokens, restClientBuilder, webClientBuilder);
            case "OLLAMA" -> new OllamaInvoker(apiKey, baseUrl, modelName,
                    temperature, maxTokens, restClientBuilder, webClientBuilder);
            case "DEEPSEEK" -> new DeepSeekInvoker(apiKey, baseUrl, modelName,
                    temperature, maxTokens, restClientBuilder, webClientBuilder);
            case "CUSTOM" -> new CustomInvoker(apiKey, baseUrl, modelName,
                    temperature, maxTokens, restClientBuilder, webClientBuilder);
            default -> throw new IllegalArgumentException("Unsupported platform type: " + platformType);
        };
    }
}
