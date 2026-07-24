package com.ghost616.platform.controller;

import com.ghost616.platform.dto.ApiResponse;
import com.ghost616.platform.dto.tool.ToolDetailDTO;
import com.ghost616.platform.service.tool.ToolConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/browser-tool")
@RequiredArgsConstructor
public class BrowserToolController {

    private final ToolConfigService toolConfigService;

    @PostMapping("/pass-result")
    public ApiResponse<Void> passResult(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(null);
    }

    @GetMapping("/extension")
    public String getExtension() {
        return EXTENSION_JS;
    }

    @GetMapping("/tool-script/{toolConfigId}")
    public ApiResponse<String> getToolScript(@PathVariable Long toolConfigId) {
        ToolDetailDTO toolDetail = toolConfigService.getById(toolConfigId);
        if (toolDetail == null) {
            return ApiResponse.fail("TOOL-001", "Tool config not found: " + toolConfigId);
        }
        return ApiResponse.success(toolDetail.getToolScript());
    }

    private static final String EXTENSION_JS = """
(function() {
    if (typeof ToolHostBridge === 'undefined') {
        return;
    }

    ToolHostBridge.getAgentExecutionContext = async function(sessionId) {
        const response = await fetch('/api/context/' + sessionId);
        const data = await response.json();
        if (data.success) {
            return data.data;
        }
        throw new Error('Failed to get agent execution context: ' + data.message);
    };

    ToolHostBridge.passToolResult = function(sessionId, toolId, result) {
        fetch('/api/browser-tool/pass-result', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: sessionId, toolId: toolId, result: result })
        });
    };

    ToolHostBridge.putSessionVariable = function(sessionId, key, value) {
        fetch('/api/context/' + sessionId + '/session-variable', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key: key, value: value })
        });
    };

    ToolHostBridge.putConversationVariable = function(sessionId, key, value) {
        fetch('/api/context/' + sessionId + '/conversation-variable', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key: key, value: value })
        });
    };
})();
""";
}
