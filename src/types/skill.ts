import type { CommonStatus } from './common';

export interface SkillConfig {
  id: string;
  name: string;
  description?: string;
  prompt: string;
  status: CommonStatus;
  toolIds: string[];
  createTime: string;
  updateTime: string;
}

export interface SkillFormData {
  name: string;
  description?: string;
  prompt: string;
  toolIds?: string[];
}

export interface SkillListParams {
  name?: string;
  status?: CommonStatus;
}
