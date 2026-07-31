package com.ghost616.agentinteg.model.invoker;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 火山引擎平台 Responses API 模型调用器（与 OpenAI Responses 兼容）。
 */
public class VolcEngineResponsesInvoker extends OpenAIResponsesInvoker {

    public VolcEngineResponsesInvoker(String apiKey, String baseUrl, String modelName,
            Double defaultTemperature, Integer defaultMaxTokens,
            RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        super(apiKey, baseUrl, modelName, defaultTemperature, defaultMaxTokens,
                restClientBuilder, webClientBuilder);
    }
}
