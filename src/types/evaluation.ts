export interface Evaluation {
  id: string;
  name: string;
  description?: string;
  agentEvalId: string;
  agentId: string;
  agentName?: string;
  executionCount: number;
  modelId: string;
  executionType?: string;
  benchmarkSessionId?: string;
  createTime?: string;
  updateTime?: string;
}

export interface EvaluationCreateRequest {
  name: string;
  description?: string;
  agentEvalId: string;
  executionCount: number;
  modelId: string;
  executionType?: string;
}

export interface EvaluationUpdateRequest {
  name?: string;
  description?: string;
  executionCount?: number;
  modelId?: string;
  executionType?: string;
}

export interface EvaluationResult {
  id: string;
  evaluationId: string;
  evaluationSessionId: string;
  result?: string;
  totalTokenUsed?: string;
  executionStatus?: string;
  modelId?: string;
  finalScore?: number;
  createTime?: string;
}

export interface ExecutionStatusResponse {
  evaluationId?: string;
  executionSessionId?: string;
}

export interface CacheStatusResponse {
  hasCache: boolean;
  cacheId?: string;
}

export interface GenerateStatusResponse {
  status: string;
  currentStep?: number;
  totalSteps?: number;
}

export interface EvalSessionCreateResponse {
  sessionId: string;
  userMessages: string[];
}
