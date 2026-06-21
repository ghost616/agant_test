import type { CommonStatus } from './common';

export interface AgentConfig {
  id: string;
  name: string;
  description?: string;
  systemPrompt?: string;
  modelId?: string;
  status: CommonStatus;
  toolIds: string[];
  createTime: string;
  updateTime: string;
}

export interface AgentFormData {
  name: string;
  description?: string;
  systemPrompt?: string;
  modelId?: string;
  toolIds?: string[];
}

export interface AgentListParams {
  name?: string;
  status?: CommonStatus;
}
