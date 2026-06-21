import type { ApiResponse, CommonStatus } from '../types/common';
import type { ToolConfig, ToolFormData } from '../types/tool';
import api from './api';

export interface ToolListParams {
  name?: string;
  toolType?: string;
  status?: string;
}

export async function listTools(params: ToolListParams): Promise<ToolConfig[]> {
  const res = await api.get<ApiResponse<ToolConfig[]>>('/tools', { params });
  return res.data.data;
}

export async function getTool(id: string): Promise<ToolConfig> {
  const res = await api.get<ApiResponse<ToolConfig>>(`/tools/${id}`);
  return res.data.data;
}

export async function createTool(data: ToolFormData): Promise<ToolConfig> {
  const res = await api.post<ApiResponse<ToolConfig>>('/tools', data);
  return res.data.data;
}

export async function updateTool(
  id: string,
  data: Partial<ToolFormData>,
): Promise<ToolConfig> {
  const res = await api.put<ApiResponse<ToolConfig>>(`/tools/${id}`, data);
  return res.data.data;
}

export async function deleteTool(id: string): Promise<void> {
  await api.delete(`/tools/${id}`);
}

export async function updateToolStatus(
  id: string,
  status: CommonStatus,
): Promise<void> {
  await api.patch(`/tools/${id}/status`, { status });
}
