export interface ToolCall {
    id?: string;
    name?: string;
    arguments?: string;
}

export interface HistoryEntry {
    role: string;
    content?: string;
    reasoning?: string;
    toolCallId?: string;
    sequenceNum: number;
    createTime?: string;
    toolCalls?: ToolCall[];
}

export interface ToolInfo {
    name: string;
    description?: string;
    parameterSchema?: string;
}

export interface SkillInfo {
    name: string;
    description?: string;
    prompt?: string;
    skillTools?: ToolInfo[];
}

export interface AgentExecutionContext {
    sessionId?: string;
    agentId?: string;
    systemPrompt?: string;
    modelId?: string;
    recentMessageCount?: number;
    history?: HistoryEntry[];
    tools?: ToolInfo[];
    skills?: SkillInfo[];
    sessionVariables?: Record<string, string>;
    conversationVariables?: Record<string, string>;
}

interface ReadArgs {
    key?: string;
}

export function execute(_ctx: AgentExecutionContext, args: string): string {
    let parsed: ReadArgs;
    try {
        parsed = JSON.parse(args) as ReadArgs;
    } catch (e: unknown) {
        const msg = e instanceof Error ? e.message : String(e);
        return JSON.stringify({ error: `Argument parse error: ${msg}` });
    }

    const key = parsed.key?.trim();
    if (!key) {
        return JSON.stringify({ error: 'key 参数不能为空' });
    }

    const value = _ctx.sessionVariables?.[key] ?? null;

    return JSON.stringify({ key, value, exists: value !== null });
}
