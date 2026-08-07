import type { CommonStatus } from './common';

export type PublishStatus =
  | 'UNPUBLISHED'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'PENDING_PUBLISH'
  | 'PUBLISH_ERROR';

export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  status: CommonStatus;
  vectorModelId?: string;
  esIndex?: string;
  rebuilding?: boolean;
  createTime: string;
  updateTime: string;
}

export interface KnowledgeFile {
  id: string;
  fileName: string;
  fileDescription?: string;
  knowledgeBaseId: string;
  fileSize?: number;
  lineCount?: number;
  status: CommonStatus;
  publishStatus?: PublishStatus;
  createTime: string;
  updateTime: string;
}

export type KBFormData = Omit<
  KnowledgeBase,
  'id' | 'status' | 'rebuilding' | 'createTime' | 'updateTime'
>;

export interface KFFormData {
  fileName: string;
  fileDescription?: string;
}

export interface KBListParams {
  name?: string;
  status?: string;
}

export interface KFListParams {
  fileName?: string;
  status?: string;
}
