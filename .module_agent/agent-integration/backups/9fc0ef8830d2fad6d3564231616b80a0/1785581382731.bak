package com.ghost616.agentinteg.model.invoker;

import java.util.Map;

import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentinteg.model.KimiModel;

/**
 * Kimi 平台模型调用器（与 OpenAI 完全兼容）。
 */
public class KimiInvoker extends OpenAIInvoker {

    public KimiInvoker(String apiKey, String baseUrl, String modelName,
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
            body.remove("thinking");
        } else if (effectiveModel.startsWith(KimiModel.K3.getModelName())) {
            body.remove("thinking");
            if (Boolean.TRUE.equals(request.getThinking())) {
                body.put("reasoning_effort", "max");
            }
        }
        return body;
    }
}
