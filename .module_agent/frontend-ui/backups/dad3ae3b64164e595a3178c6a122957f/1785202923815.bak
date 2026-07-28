import type { ApiResponse } from '../types/common';
import type {
  Evaluation,
  EvaluationCreateRequest,
  EvaluationUpdateRequest,
  EvaluationResult,
} from '../types/evaluation';
import api from './api';

export async function getEvaluationList(): Promise<Evaluation[]> {
  const res = await api.get<ApiResponse<Evaluation[]>>('/evaluations');
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
