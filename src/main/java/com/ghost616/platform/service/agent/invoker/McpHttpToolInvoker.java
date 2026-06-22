package com.ghost616.platform.service.agent.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.service.agent.AgentExecutionContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class McpHttpToolInvoker implements ToolInvoker {

    private final String mcpServerUrl;
    private final String toolName;
    private final McpJsonRpcClient client;

    public McpHttpToolInvoker(String mcpServerUrl, String toolName, String authConfig) {
        this.mcpServerUrl = mcpServerUrl;
        this.toolName = toolName;
        Map<String, String> headers = McpAuthConfigParser.parse(authConfig);
        this.client = new McpJsonRpcClient(mcpServerUrl, headers);
        log.debug("MCP HTTP 调用器初始化: url={}, tool={}", mcpServerUrl, toolName);
    }

    @Override
    public String execute(AgentExecutionContext ctx, String arguments) {
        try {
            client.initialize();

            Map<String, Object> argsMap = parseArguments(arguments);
            Map<String, Object> result = client.callTool(toolName, argsMap);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("content");
            if (contents == null || contents.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> content : contents) {
                if ("text".equals(content.get("type"))) {
                    sb.append(content.get("text"));
                } else {
                    sb.append(content.toString());
                }
            }
            return sb.toString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MCP HTTP 工具执行异常: url={}, tool={}", mcpServerUrl, toolName, e);
            throw new BusinessException(ErrorCode.TOOL_EXECUTE_ERROR,
                    "MCP HTTP 工具执行异常: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.readValue(arguments, Map.class);
            return result == null ? Map.of() : result;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOOL_INVOKE_ERROR,
                    "MCP 工具参数解析失败: " + e.getMessage());
        }
    }
}
