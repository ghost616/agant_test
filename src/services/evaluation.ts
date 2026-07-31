import type { ApiResponse } from '../types/common';
import type {
  Evaluation,
  EvaluationCreateRequest,
  EvaluationUpdateRequest,
  EvaluationResult,
  ExecutionStatusResponse,
  EvalSessionCreateResponse,
  GenerateStatusResponse,
} from '../types/evaluation';
import api from './api';

export async function getEvaluationList(
  agentEvalId?: string,
): Promise<Evaluation[]> {
  const params = agentEvalId ? { agentEvalId } : undefined;
  const res = await api.get<ApiResponse<Evaluation[]>>('/evaluations', {
    params,
  });
  return res.data.data;
}

export async function getEvaluation(id: string): Promise<Evaluation> {
  const res = await api.get<ApiResponse<Evaluation>>(`/evaluations/${id}`);
  return res.data.data;
}

export async function createEvaluation(
  data: EvaluationCreateRequest,
): Promise<Evaluation> {
  const res = await api.post<ApiResponse<Evaluation>>('/evaluations', data);
  return res.data.data;
}

export async function updateEvaluation(
  id: string,
  data: EvaluationUpdateRequest,
): Promise<Evaluation> {
  const res = await api.put<ApiResponse<Evaluation>>(
    `/evaluations/${id}`,
    data,
  );
  return res.data.data;
}

export async function deleteEvaluation(id: string): Promise<void> {
  await api.delete(`/evaluations/${id}`);
}

export async function getEvaluationResults(
  evaluationId: string,
): Promise<EvaluationResult[]> {
  const res = await api.get<ApiResponse<EvaluationResult[]>>(
    `/evaluations/${evaluationId}/results`,
  );
  return res.data.data;
}

export async function executeEvaluation(id: string): Promise<void> {
  await api.post(`/evaluations/${id}/execute`);
}

export async function getExecutionStatus(
  id: string,
): Promise<ExecutionStatusResponse> {
  const res = await api.get<ApiResponse<ExecutionStatusResponse>>(
    `/evaluations/${id}/execute/status`,
  );
  return res.data.data;
}

export async function createEvalSession(
  id: string,
): Promise<EvalSessionCreateResponse> {
  const res = await api.post<ApiResponse<EvalSessionCreateResponse>>(
    `/evaluations/${id}/session`,
  );
  return res.data.data;
}

export async function getEvaluationResult(id: string): Promise<EvaluationResult> {
  const res = await api.get<ApiResponse<EvaluationResult>>(`/evaluations/results/${id}`);
  return res.data.data;
}

export async function generateEvalResult(
  id: string,
  sessionId: string,
): Promise<void> {
  await api.post(`/evaluations/${id}/session/${sessionId}/generate`);
}

export async function getGenerateStatus(
  id: string,
  sessionId: string,
): Promise<GenerateStatusResponse> {
  const res = await api.get<ApiResponse<GenerateStatusResponse>>(
    `/evaluations/${id}/session/${sessionId}/generate/status`,
  );
  return res.data.data;
}

export async function deleteEvaluationResult(id: string): Promise<void> {
  await api.delete(`/evaluations/results/${id}`);
}

export async function batchDeleteEvaluationResults(ids: string[]): Promise<void> {
  await api.post('/evaluations/results/batch-delete', ids);
}

export async function clearEvaluationResults(evaluationId: string): Promise<void> {
  await api.delete(`/evaluations/${evaluationId}/results`);
}
