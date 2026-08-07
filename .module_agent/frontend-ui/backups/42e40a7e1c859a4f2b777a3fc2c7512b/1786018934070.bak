import type { CommonStatus } from './common';

export type SessionAuthType = 'ALL' | 'PARENT' | 'CHILD';

export interface AgentConfig {
  id: string;
  name: string;
  description?: string;
  systemPrompt?: string;
  modelId?: string;
  status: CommonStatus;
  tools: { toolId: string; sessionAuth: SessionAuthType }[];
  skills: { skillId: string; sessionAuth: SessionAuthType }[];
  recentMessageCount?: number;
  createTime: string;
  updateTime: string;
}

export interface AgentFormData {
  name: string;
  description?: string;
  systemPrompt?: string;
  modelId?: string;
  tools?: { toolId: string; sessionAuth: SessionAuthType }[];
  skills?: { skillId: string; sessionAuth: SessionAuthType }[];
  recentMessageCount?: number;
}

export interface AgentListParams {
  name?: string;
  status?: CommonStatus;
}
