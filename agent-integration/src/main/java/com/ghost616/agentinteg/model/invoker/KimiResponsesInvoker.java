package com.ghost616.agentinteg.model.invoker;

import java.util.Map;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentinteg.model.KimiModel;

/**
 * Kimi 平台 Responses API 模型调用器。
 */
public class KimiResponsesInvoker extends OpenAIResponsesInvoker {

    public KimiResponsesInvoker(String apiKey, String baseUrl, String modelName,
            Double defaultTemperature, Integer defaultMaxTokens,
            RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        super(apiKey, baseUrl, modelName, defaultTemperature, defaultMaxTokens,
                restClientBuilder, webClientBuilder);
    }

    @Override
    protected Map<String, Object> buildRequestBody(ChatRequest request, boolean stream) {
        Map<String, Object> body = super.buildRequestBody(request, stream);
        String effectiveModel = request.getModel() != null ? request.getModel() : modelName;
        if (effectiveModel.startsWith(KimiModel.K2_7_CODE.getModelName())) {
            body.remove("reasoning");
        } else if (effectiveModel.startsWith(KimiModel.K3.getModelName())) {
            body.remove("reasoning");
            if (Boolean.TRUE.equals(request.getThinking())) {
                Map<String, Object> reasoning = buildReasoning();
                reasoning.put("effort", "max");
                body.put("reasoning", reasoning);
            }
        }
        return body;
    }
}
