export interface Session {
  id: string;
  agentId: string;
  modelId: string;
  title: string;
  systemPrompt?: string;
  parentSessionId?: string;
  isChild?: boolean;
  createTime: string;
  updateTime: string;
  totalTokenUsed?: string;
}

export interface CreateSessionParams {
  agentId: string;
  modelId: string;
  title: string;
}

export interface ToolCallData {
  toolCallId: string;
  toolCallName: string;
  toolCallArguments: string;
}

export interface SessionMessage {
  id: string;
  sessionId: string;
  role: string;
  content: string;
  reasoning?: string;
  toolResult?: string;
  toolCallId?: string;
  sequenceNum: number;
  createTime: string;
  toolCalls?: ToolCallData[];
}
