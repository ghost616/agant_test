package com.ghost616.agentinteg.model.invoker;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 火山引擎平台模型调用器（与 OpenAI 完全兼容）。
 */
public class VolcEngineInvoker extends OpenAIInvoker {

    public VolcEngineInvoker(String apiKey, String baseUrl, String modelName,
            Double defaultTemperature, Integer defaultMaxTokens,
            RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        super(apiKey, baseUrl, modelName, defaultTemperature, defaultMaxTokens,
                restClientBuilder, webClientBuilder);
    }
}
