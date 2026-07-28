import type { ApiResponse } from '../types/common';
import type {
  AgentEvaluation,
  AgentEvaluationCreateRequest,
  AgentEvaluationUpdateRequest,
} from '../types/agentEvaluation';
import api from './api';

export async function getAgentEvaluationList(): Promise<AgentEvaluation[]> {
  const res = await api.get<ApiResponse<AgentEvaluation[]>>(
    '/agent-evaluations',
  );
  return res.data.data;
}

export async function getAgentEvaluation(
  id: string,
): Promise<AgentEvaluation> {
  const res = await api.get<ApiResponse<AgentEvaluation>>(
    `/agent-evaluations/${id}`,
  );
  return res.data.data;
}

export async function createAgentEvaluation(
  data: AgentEvaluationCreateRequest,
): Promise<AgentEvaluation> {
  const res = await api.post<ApiResponse<AgentEvaluation>>(
    '/agent-evaluations',
    data,
  );
  return res.data.data;
}

export async function updateAgentEvaluation(
  id: string,
  data: AgentEvaluationUpdateRequest,
): Promise<AgentEvaluation> {
  const res = await api.put<ApiResponse<AgentEvaluation>>(
    `/agent-evaluations/${id}`,
    data,
  );
  return res.data.data;
}

export async function deleteAgentEvaluation(id: string): Promise<void> {
  await api.delete(`/agent-evaluations/${id}`);
}

