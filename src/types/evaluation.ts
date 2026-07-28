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
  createTime?: string;
}
