import type { CommonStatus } from './common';

export type SessionAuthType = 'ALL' | 'PARENT' | 'CHILD';

export interface KnowledgeBaseItem {
  id: string;
  name: string;
}

export interface AgentConfig {
  id: string;
  name: string;
  description?: string;
  systemPrompt?: string;
  modelId?: string;
  status: CommonStatus;
  tools: { toolId: string; sessionAuth: SessionAuthType }[];
  skills: { skillId: string; sessionAuth: SessionAuthType }[];
  knowledgeBases?: KnowledgeBaseItem[];
  recentMessageCount?: number;
  memoryEnabled?: boolean;
  memoryGroupCount?: number;
  vectorModelId?: string;
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
  knowledgeBaseIds?: string[];
  recentMessageCount?: number;
  memoryEnabled?: boolean;
  memoryGroupCount?: number;
  vectorModelId?: string;
}

export interface AgentListParams {
  name?: string;
  status?: CommonStatus;
}
