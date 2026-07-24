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
