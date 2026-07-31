package com.ghost616.agentinteg.model.invoker;

import com.ghost616.agentbase.dto.model.ModelConfigData;
import com.ghost616.agentbase.enums.RequestType;
import com.ghost616.agentbase.service.model.invoker.ModelInvoker;
import com.ghost616.agentbase.service.model.invoker.ModelInvokerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
public class DefaultModelInvokerFactory implements ModelInvokerFactory {

    private final RestClient.Builder restClientBuilder;
    private final WebClient.Builder webClientBuilder;

    @Override
    public ModelInvoker createInvoker(ModelConfigData config) {
        String requestType = config.requestType();
        if (RequestType.isResponses(requestType) && supportsResponses(config.platformType())) {
            return createResponsesInvoker(config);
        }
        if (RequestType.COMPLETIONS.getCode().equals(requestType) || !supportsResponses(config.platformType())) {
            return createChatCompletionsInvoker(config);
        }
        return createChatCompletionsInvoker(config);
    }

    private boolean supportsResponses(String platformType) {
        return switch (platformType) {
            case "OPENAI", "DEEPSEEK", "KIMI", "VOLCENGINE", "AZURE", "CUSTOM" -> true;
            default -> false;
        };
    }

    private ModelInvoker createResponsesInvoker(ModelConfigData config) {
        return switch (config.platformType()) {
            case "OPENAI" -> new OpenAIResponsesInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "DEEPSEEK" -> new DeepSeekResponsesInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "KIMI" -> new KimiResponsesInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "VOLCENGINE" -> new VolcEngineResponsesInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "AZURE" -> new AzureResponsesInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "CUSTOM" -> new CustomResponsesInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            default -> throw new IllegalArgumentException(
                    "Unsupported responses platform type: " + config.platformType());
        };
    }

    private ModelInvoker createChatCompletionsInvoker(ModelConfigData config) {
        return switch (config.platformType()) {
            case "OPENAI" -> new OpenAIInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "ANTHROPIC" -> new AnthropicInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "AZURE" -> new AzureInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "OLLAMA" -> new OllamaInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "KIMI" -> new KimiInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "VOLCENGINE" -> new VolcEngineInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "DEEPSEEK" -> new DeepSeekInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            case "CUSTOM" -> new CustomInvoker(config.apiKey(), config.baseUrl(), config.modelName(),
                    config.temperature(), config.maxTokens(), restClientBuilder, webClientBuilder);
            default -> throw new IllegalArgumentException("Unsupported platform type: " + config.platformType());
        };
    }
}
