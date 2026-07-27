// ============================================================
// ToolHostBridge - Host Function Bridge
// ============================================================
const ToolHostBridge = {
    getAgentExecutionContext: function (sessionId) {
        throw new Error("ToolHostBridge.getAgentExecutionContext not implemented");
    },

    passToolResult: function (sessionId, toolId, result) {
        throw new Error("ToolHostBridge.passToolResult not implemented");
    },

    putSessionVariable: function (sessionId, key, value) {
        throw new Error("ToolHostBridge.putSessionVariable not implemented");
    },

    putConversationVariable: function (sessionId, key, value) {
        throw new Error("ToolHostBridge.putConversationVariable not implemented");
    }
};

// ============================================================
// AgentExecutionContext
// ============================================================
const AgentExecutionContext = {
    sessionId: null,
    agentId: null,
    systemPrompt: null,
    modelId: null,
    parentSessionId: null,
    recentMessageCount: null,
    history: [],
    tools: [],
    skills: [],
    projectDir: null,
    sessionVariables: {},
    conversationVariables: {},

    putSessionVariable: function (key, value) {
        this.sessionVariables[key] = value;
        ToolHostBridge.putSessionVariable(this.sessionId, key, value);
    },

    getSessionVariable: function (key) {
        return this.sessionVariables[key];
    },

    putConversationVariable: function (key, value) {
        this.conversationVariables[key] = value;
        ToolHostBridge.putConversationVariable(this.sessionId, key, value);
    },

    getConversationVariable: function (key) {
        return this.conversationVariables[key];
    },

    isMainSession: function () {
        return this.parentSessionId === null;
    }
};

// ============================================================
// Tool Function Definitions
// ============================================================
const ToolFunction = {
    name: null,
    description: null,
    parameters: null,
    handler: null,

    create: function (name, description, parameters, handler) {
        return {
            name: name,
            description: description,
            parameters: parameters,
            handler: handler
        };
    }
};

// ============================================================
// Tool Function Manager
// ============================================================
const ToolManager = {
    _tools: {},

    bind: function (tools) {
        this._tools = {};
        if (Array.isArray(tools)) {
            for (const tool of tools) {
                if (tool && tool.name) {
                    this._tools[tool.name] = tool;
                }
            }
        }
    },

    add: function (tool) {
        if (tool && tool.name) {
            this._tools[tool.name] = tool;
        }
    },

    remove: function (toolName) {
        delete this._tools[toolName];
    },

    get: function (toolName) {
        return this._tools[toolName];
    },

    getAll: function () {
        return Object.values(this._tools);
    },

    has: function (toolName) {
        return toolName in this._tools;
    }
};

// ============================================================
// ToolExecutor - Execution Functions
// ============================================================
const ToolExecutor = {

    getAgentExecutionContext: function (sessionId) {
        return ToolHostBridge.getAgentExecutionContext(sessionId);
    },

    getToolResult: function (agentExecContext, toolName, params) {
        const tool = ToolManager.get(toolName);
        if (!tool) {
            throw new Error("Tool not found: " + toolName);
        }
        if (typeof tool.handler !== "function") {
            throw new Error("Tool has no handler: " + toolName);
        }
        return tool.handler(agentExecContext, params);
    },

    passToolResult: function (sessionId, toolId, result) {
        ToolHostBridge.passToolResult(sessionId, toolId, result);
    },

    execute: function (sessionId, toolId, toolName, params) {
        const tool = ToolManager.get(toolName);
        if (!tool) {
            throw new Error("Tool not found: " + toolName);
        }
        if (typeof tool.handler !== "function") {
            throw new Error("Tool has no handler: " + toolName);
        }
        const agentCtx = this.getAgentExecutionContext(sessionId);
        const result = tool.handler(agentCtx, params);
        this.passToolResult(sessionId, toolId, result);
        return result;
    }
};
