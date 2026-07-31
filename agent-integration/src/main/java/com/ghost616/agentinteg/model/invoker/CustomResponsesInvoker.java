package com.ghost616.agentinteg.model.invoker;

import java.util.Map;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 自定义平台 Responses API 模型调用器（通用 OpenAI Responses 兼容端点）。
 */
public class CustomResponsesInvoker extends OpenAIResponsesInvoker {

    public CustomResponsesInvoker(String apiKey, String baseUrl, String modelName,
            Double defaultTemperature, Integer defaultMaxTokens,
            RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        super(apiKey, baseUrl, modelName, defaultTemperature, defaultMaxTokens,
                restClientBuilder, webClientBuilder);
    }
}
