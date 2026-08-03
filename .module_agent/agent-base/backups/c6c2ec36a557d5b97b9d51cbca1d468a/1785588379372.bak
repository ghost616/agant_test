// _runner.ts - Auto-generated bridge file for TypeScriptToolInvoker

import { readFileSync } from 'fs';

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
    stopped?: boolean;
    projectDir?: string;
}

export interface ExecuteInput {
    context: AgentExecutionContext;
    arguments: string;
}

export interface VariableChanges {
    added: Record<string, string>;
    removed: string[];
}

function createVariableProxy(snapshot: Record<string, string>): {
    proxy: Record<string, string>;
    getChanges: () => VariableChanges;
} {
    const working = { ...snapshot };
    const proxy = new Proxy(working, {
        set(_target, key: string, value) {
            working[key] = value;
            return true;
        },
        deleteProperty(_target, key: string) {
            delete working[key];
            return true;
        }
    });
    return {
        proxy,
        getChanges: () => {
            const added: Record<string, string> = {};
            const removed: string[] = [];
            for (const key of Object.keys(working)) {
                if (!(key in snapshot) || snapshot[key] !== working[key]) {
                    added[key] = working[key];
                }
            }
            for (const key of Object.keys(snapshot)) {
                if (!(key in working)) {
                    removed.push(key);
                }
            }
            return { added, removed };
        }
    };
}

async function main() {
    const inputPath = process.argv[2];
    if (!inputPath) {
        console.error(JSON.stringify({ error: 'Missing input file path argument' }));
        process.exit(1);
    }
    let jsonStr: string;
    try {
        jsonStr = readFileSync(inputPath, 'utf-8');
    } catch (e: any) {
        console.error(JSON.stringify({ error: 'Failed to read input file: ' + e.message }));
        process.exit(1);
    }

    let input: ExecuteInput;
    try {
        input = JSON.parse(jsonStr);
    } catch (e: any) {
        console.error(JSON.stringify({ error: 'JSON parse error: ' + e.message }));
        process.exit(1);
    }

    const { context, arguments: args } = input;

    const sessionVarSnapshot = { ...(context.sessionVariables || {}) };
    const conversationVarSnapshot = { ...(context.conversationVariables || {}) };

    const sessionVarWrap = createVariableProxy(sessionVarSnapshot);
    const conversationVarWrap = createVariableProxy(conversationVarSnapshot);

    context.sessionVariables = sessionVarWrap.proxy;
    context.conversationVariables = conversationVarWrap.proxy;

    try {
        const module = await import('./index');
        if (typeof module.execute !== 'function') {
            throw new Error('index module does not export execute function');
        }
        const rawResult = await module.execute(context, args);

        const sessionChanges = sessionVarWrap.getChanges();
        const conversationChanges = conversationVarWrap.getChanges();

        const output = {
            result: typeof rawResult === 'string' ? rawResult : JSON.stringify(rawResult),
            sessionVariables: sessionChanges,
            conversationVariables: conversationChanges,
        };

        process.stdout.write(JSON.stringify(output));
    } catch (e: any) {
        const errMsg = e instanceof Error ? e.message : String(e);
        console.error(JSON.stringify({ error: errMsg }));
        process.exit(1);
    }
}

main();
