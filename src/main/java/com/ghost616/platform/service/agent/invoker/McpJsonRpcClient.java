package com.ghost616.platform.service.agent.invoker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class McpJsonRpcClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String serverUrl;
    private final Map<String, String> headers;
    private final ObjectMapper objectMapper;

    public McpJsonRpcClient(String serverUrl, Map<String, String> headers) {
        this.serverUrl = serverUrl;
        this.headers = headers != null ? headers : Map.of();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void initialize() {
        Map<String, Object> request = buildRequest("initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of(
                        "name", "agent-platform",
                        "version", "1.0.0"
                )
        ));
        sendRequest(request);
    }

    public List<Map<String, Object>> listTools() {
        Map<String, Object> request = buildRequest("tools/list", Map.of());
        Map<String, Object> result = sendRequest(request);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        return tools != null ? tools : List.of();
    }

    public Map<String, Object> callTool(String name, Map<String, Object> arguments) {
        Map<String, Object> request = buildRequest("tools/call", Map.of(
                "name", name,
                "arguments", arguments != null ? arguments : Map.of()
        ));
        return sendRequest(request);
    }

    private Map<String, Object> buildRequest(String method, Map<String, Object> params) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", UUID.randomUUID().toString(),
                "method", method,
                "params", params
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendRequest(Map<String, Object> request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.TOOL_EXECUTE_ERROR,
                        "MCP HTTP 请求失败: HTTP " + response.statusCode() + " - " + response.body());
            }

            String responseBody = response.body();
            String contentType = response.headers().firstValue("Content-Type").orElse("");

            if (contentType.contains("text/event-stream")) {
                responseBody = extractSseData(responseBody);
            }

            Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                    new TypeReference<Map<String, Object>>() {});

            if (responseMap.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) responseMap.get("error");
                throw new BusinessException(ErrorCode.TOOL_EXECUTE_ERROR,
                        "MCP JSON-RPC 错误: " + error.get("message"));
            }

            return (Map<String, Object>) responseMap.get("result");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MCP JSON-RPC 通信异常: url={}", serverUrl, e);
            throw new BusinessException(ErrorCode.TOOL_EXECUTE_ERROR,
                    "MCP JSON-RPC 通信异常: " + e.getMessage());
        }
    }

    private String extractSseData(String body) {
        return body.lines()
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .collect(Collectors.joining());
    }
}
