package com.ghost616.platform.service.model.invoker;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class DefaultModelInvokerFactory implements ModelInvokerFactory {

    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;

    @Override
    public ModelInvoker createInvoker(ModelConfigData config) {
        return switch (config.platformType()) {
            case "OPENAI" -> new OpenAIInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "ANTHROPIC" -> new AnthropicInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "AZURE" -> new AzureInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "OLLAMA" -> new OllamaInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "DEEPSEEK" -> new DeepSeekInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "CUSTOM" -> new CustomInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            default -> throw new IllegalArgumentException("Unsupported platform type: " + config.platformType());
        };
    }
}
