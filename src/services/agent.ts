import type { ApiResponse, CommonStatus } from '../types/common';
import type { AgentConfig, AgentFormData, AgentListParams } from '../types/agent';
import api from './api';

export async function listAgents(params?: AgentListParams): Promise<AgentConfig[]> {
  const res = await api.get<ApiResponse<AgentConfig[]>>('/agents', { params });
  return res.data.data;
}

export async function getAgent(id: string): Promise<AgentConfig> {
  const res = await api.get<ApiResponse<AgentConfig>>(`/agents/${id}`);
  return res.data.data;
}

export async function createAgent(data: AgentFormData): Promise<AgentConfig> {
  const res = await api.post<ApiResponse<AgentConfig>>('/agents', data);
  return res.data.data;
}

export async function updateAgent(
  id: string,
  data: AgentFormData,
): Promise<AgentConfig> {
  const res = await api.put<ApiResponse<AgentConfig>>(`/agents/${id}`, data);
  return res.data.data;
}

export async function deleteAgent(id: string): Promise<void> {
  await api.delete(`/agents/${id}`);
}

export async function updateAgentStatus(
  id: string,
  status: CommonStatus,
): Promise<void> {
  await api.put(`/agents/${id}/status`, { status });
}
