package com.ghost616.platform.service.model.invoker;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * DeepSeek 平台模型调用器（与 OpenAI 完全兼容）。
 */
public class DeepSeekInvoker extends OpenAIInvoker {

    public DeepSeekInvoker(String apiKey, String baseUrl, String modelName,
            Double defaultTemperature, Integer defaultMaxTokens,
            RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        super(apiKey, baseUrl, modelName, defaultTemperature, defaultMaxTokens,
                restClientBuilder, webClientBuilder);
    }
}
