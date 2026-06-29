import type { ApiResponse, CommonStatus } from '../types/common';
import type { SkillConfig, SkillFormData, SkillListParams } from '../types/skill';
import api from './api';

export async function listSkills(params?: SkillListParams): Promise<SkillConfig[]> {
  const res = await api.get<ApiResponse<SkillConfig[]>>('/skills', { params });
  return res.data.data;
}

export async function getSkill(id: string): Promise<SkillConfig> {
  const res = await api.get<ApiResponse<SkillConfig>>(`/skills/${id}`);
  return res.data.data;
}

export async function createSkill(data: SkillFormData): Promise<SkillConfig> {
  const res = await api.post<ApiResponse<SkillConfig>>('/skills', data);
  return res.data.data;
}

export async function updateSkill(
  id: string,
  data: SkillFormData,
): Promise<SkillConfig> {
  const res = await api.put<ApiResponse<SkillConfig>>(`/skills/${id}`, data);
  return res.data.data;
}

export async function deleteSkill(id: string): Promise<void> {
  await api.delete(`/skills/${id}`);
}

export async function updateSkillStatus(
  id: string,
  status: CommonStatus,
): Promise<void> {
  await api.put(`/skills/${id}/status`, { status });
}
