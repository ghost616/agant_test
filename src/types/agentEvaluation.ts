export interface AgentEvaluation {
  id: string;
  name: string;
  description?: string;
  agentId: string;
  agentName?: string;
  createTime?: string;
  updateTime?: string;
}

export interface AgentEvaluationCreateRequest {
  name: string;
  description?: string;
  agentId: string;
}

export interface AgentEvaluationUpdateRequest {
  name?: string;
  description?: string;
  agentId?: string;
}
