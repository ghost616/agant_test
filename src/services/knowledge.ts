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

/**
 * 获取知识文件内容。
 * @param kbId 知识库 ID
 * @param id 知识文件 ID
 * @returns 文件内容（纯文本）
 */
export async function getKnowledgeFileContent(
  kbId: string,
  id: string,
): Promise<string> {
  const res = await api.get<ApiResponse<string>>(
    `/knowledge-bases/${kbId}/files/${id}/content`,
  );
  return res.data.data;
}

/**
 * 更新知识文件内容。
 * @param kbId 知识库 ID
 * @param id 知识文件 ID
 * @param content 新的文件内容
 */
export async function updateKnowledgeFileContent(
  kbId: string,
  id: string,
  content: string,
): Promise<void> {
  await api.put(`/knowledge-bases/${kbId}/files/${id}/content`, content, {
    headers: { 'Content-Type': 'text/plain' },
  });
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

/**
 * 发布知识文件。
 * @param kbId 知识库 ID
 * @param fileId 知识文件 ID
 */
export async function publishKnowledgeFile(
  kbId: string,
  fileId: string,
): Promise<void> {
  await api.post(`/knowledge-bases/${kbId}/files/${fileId}/publish`);
}

/**
 * 刷新知识文件列表（重新扫描知识库目录）。
 * @param kbId 知识库 ID
 */
export async function refreshKnowledgeFiles(kbId: string): Promise<void> {
  await api.put(`/knowledge-bases/${kbId}/files/refresh`);
}

/**
 * 触发知识库 ES 数据重构。
 * @param kbId 知识库 ID
 */
export async function rebuildKnowledgeBaseES(kbId: string): Promise<void> {
  await api.post(`/knowledge-bases/${kbId}/rebuild-es`);
}
