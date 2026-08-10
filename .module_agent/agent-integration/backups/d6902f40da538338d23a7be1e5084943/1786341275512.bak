package com.ghost616.agentinteg.model.invoker;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.ghost616.agentbase.dto.model.ChatChunk;
import com.ghost616.agentbase.dto.model.ChatRequest;
import com.ghost616.agentbase.dto.model.ChatResponse;
import com.ghost616.agentbase.enums.ErrorCode;
import com.ghost616.agentbase.exception.BusinessException;


/**
 * Azure OpenAI 平台模型调用器。
 */
@Slf4j
public class AzureInvoker extends OpenAIInvoker {

    private static final String API_VERSION = "2024-02-15-preview";

    public AzureInvoker(String apiKey, String baseUrl, String modelName,
            Double defaultTemperature, Integer defaultMaxTokens,
            RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder) {
        super(apiKey, baseUrl, modelName, defaultTemperature, defaultMaxTokens,
                restClientBuilder, webClientBuilder);
    }

    @Override
    protected String buildChatCompletionsUrl() {
        return baseUrl + "/openai/deployments/" + modelName
                + "/chat/completions?api-version=" + API_VERSION;
    }

    @Override
    public ChatResponse invoke(ChatRequest request) {
        try {
            String url = buildChatCompletionsUrl();
            Map<String, Object> requestBody = buildRequestBody(request, false);
            RestClient restClient = restClientBuilder.baseUrl("").build();
            String responseBody = restClient.post()
                    .uri(url)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            return parseResponse(responseBody);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Azure invoke HTTP error: status={}", e.getStatusCode().value());
            throw new BusinessException(ErrorCode.MODEL_INVOKE_ERROR,
                    "HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Azure invoke error", e);
            throw new BusinessException(ErrorCode.MODEL_INVOKE_ERROR, e.getMessage());
        }
    }

    @Override
    public Flux<ChatChunk> invokeStream(ChatRequest request) {
        try {
            String url = buildChatCompletionsUrl();
            Map<String, Object> requestBody = buildRequestBody(request, true);
            return Flux.defer(() -> {
                return webClientBuilder.baseUrl("").build()
                        .post()
                        .uri(url)
                        .header("api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .doOnSubscribe(s ->
                                log.debug("Azure stream request to {} with model {}", url, modelName))
                        .concatMap(chunkRaw -> {
                            if ("[DONE]".equals(chunkRaw)) {
                                return Mono.just(ChatChunk.builder()
                                        .finishReason("stop").build());
                            }
                            if (chunkRaw.startsWith("{")) {
                                return Mono.just(parseStreamChunk(chunkRaw));
                            }
                            return Mono.empty();
                        })
                        .onErrorResume(this::handleStreamError);
            });
        } catch (BusinessException e) {
            return Flux.error(e);
        } catch (Exception e) {
            log.error("Azure invokeStream error", e);
            return Flux.error(
                    new BusinessException(ErrorCode.MODEL_INVOKE_ERROR, e.getMessage()));
        }
    }

    @Override
    public boolean verify() {
        try {
            RestClient restClient = restClientBuilder.baseUrl("").build();
            restClient.get()
                    .uri(baseUrl + "/openai/deployments?api-version=" + API_VERSION)
                    .header("api-key", apiKey)
                    .retrieve()
                    .body(String.class);
            return true;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Azure verify HTTP error: status={}", e.getStatusCode().value());
            throw new BusinessException(ErrorCode.MODEL_VERIFY_ERROR,
                    "HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Azure verify error", e);
            throw new BusinessException(ErrorCode.MODEL_VERIFY_ERROR, e.getMessage());
        }
    }
}
