import type { ApiResponse, CommonStatus } from '../types/common';
import type {
  KBFormData,
  KFFormData,
  KBListParams,
  KFListParams,
  KnowledgeBase,
  KnowledgeFile,
} from '../types/knowledge';
import api from './api';

export async function listKnowledgeBases(
  params?: KBListParams,
): Promise<KnowledgeBase[]> {
  const res = await api.get<ApiResponse<KnowledgeBase[]>>('/knowledge-bases', {
    params,
  });
  return res.data.data;
}

export async function getKnowledgeBase(id: string): Promise<KnowledgeBase> {
  const res = await api.get<ApiResponse<KnowledgeBase>>(`/knowledge-bases/${id}`);
  return res.data.data;
}

export async function createKnowledgeBase(data: KBFormData): Promise<KnowledgeBase> {
  const res = await api.post<ApiResponse<KnowledgeBase>>('/knowledge-bases', data);
  return res.data.data;
}

export async function updateKnowledgeBase(
  id: string,
  data: Partial<KBFormData>,
): Promise<KnowledgeBase> {
  const res = await api.put<ApiResponse<KnowledgeBase>>(`/knowledge-bases/${id}`, data);
  return res.data.data;
}

export async function deleteKnowledgeBase(id: string): Promise<void> {
  await api.delete(`/knowledge-bases/${id}`);
}

export async function updateKnowledgeBaseStatus(
  id: string,
  status: CommonStatus,
): Promise<void> {
  await api.put(`/knowledge-bases/${id}/status`, null, {
    params: { status },
  });
}

export async function listKnowledgeFiles(
  kbId: string,
  params?: KFListParams,
): Promise<KnowledgeFile[]> {
  const res = await api.get<ApiResponse<KnowledgeFile[]>>(
    `/knowledge-bases/${kbId}/files`,
    { params },
  );
  return res.data.data;
}

export async function getKnowledgeFile(
  kbId: string,
  id: string,
): Promise<KnowledgeFile> {
  const res = await api.get<ApiResponse<KnowledgeFile>>(
    `/knowledge-bases/${kbId}/files/${id}`,
  );
  return res.data.data;
}

export async function createKnowledgeFile(
  kbId: string,
  data: KFFormData,
): Promise<KnowledgeFile> {
  const res = await api.post<ApiResponse<KnowledgeFile>>(
    `/knowledge-bases/${kbId}/files`,
    data,
  );
  return res.data.data;
}

export async function updateKnowledgeFile(
  kbId: string,
  id: string,
  data: Partial<KFFormData>,
): Promise<KnowledgeFile> {
  const res = await api.put<ApiResponse<KnowledgeFile>>(
    `/knowledge-bases/${kbId}/files/${id}`,
    data,
  );
  return res.data.data;
}

export async function deleteKnowledgeFile(kbId: string, id: string): Promise<void> {
  await api.delete(`/knowledge-bases/${kbId}/files/${id}`);
}

export async function updateKnowledgeFileStatus(
  kbId: string,
  id: string,
  status: CommonStatus,
): Promise<void> {
  await api.put(`/knowledge-bases/${kbId}/files/${id}/status`, null, {
    params: { status },
  });
}
