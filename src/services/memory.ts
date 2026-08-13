import type { ApiResponse, PageResult } from '../types/common';
import type {
  MemoryAggregationType,
  MemoryPromptSaveRequest,
  MemoryRegenerateRequest,
  MemoryRegenerateStatus,
  MemoryUpdateRequest,
  SessionMemoryDocument,
} from '../types/memory';
import api from './api';

/**
 * 分页查询会话记忆聚合文档。
 * @param sessionId 会话 ID
 * @param type 聚合类型（GROUP/DAILY）
 * @param page 页码（从 1 开始）
 * @param size 每页条数
 * @returns 分页记忆聚合结果
 */
export async function getSessionMemory(
  sessionId: string,
  type: MemoryAggregationType,
  page: number,
  size: number,
): Promise<PageResult<SessionMemoryDocument>> {
  const res = await api.get<ApiResponse<PageResult<SessionMemoryDocument>>>(
    `/sessions/${sessionId}/memory`,
    { params: { type, page, size } },
  );
  return res.data.data;
}

/**
 * 获取会话已保存的记忆提示语。
 * @param sessionId 会话 ID
 * @returns 提示语字符串（未配置时可能为空）
 */
export async function getMemoryPrompt(sessionId: string): Promise<string> {
  const res = await api.get<ApiResponse<string>>(`/sessions/${sessionId}/memory-prompt`);
  return res.data.data;
}

/**
 * 保存会话记忆提示语。
 * @param sessionId 会话 ID
 * @param prompt 提示语内容
 */
export async function saveMemoryPrompt(sessionId: string, prompt: string): Promise<void> {
  const body: MemoryPromptSaveRequest = { prompt };
  await api.put<ApiResponse<void>>(`/sessions/${sessionId}/memory-prompt`, body);
}

/**
 * 保存聚合文本：重新向量化并更新 ES。
 * @param sessionId 会话 ID
 * @param docId 目标 ES 文档 ID
 * @param text 新的聚合摘要文本
 */
export async function updateMemoryDocument(
  sessionId: string,
  docId: string,
  text: string,
): Promise<void> {
  const body: MemoryUpdateRequest = { docId, text };
  await api.post<ApiResponse<void>>(`/sessions/${sessionId}/memory/update`, body);
}

/**
 * 触发聚合文本异步重生成。
 * @param sessionId 会话 ID
 * @param request 重生成请求（docId/startSeq/endSeq/prompt）
 * @returns 初始重生成状态（RUNNING）
 */
export async function regenerateMemory(
  sessionId: string,
  request: MemoryRegenerateRequest,
): Promise<MemoryRegenerateStatus> {
  const res = await api.post<ApiResponse<MemoryRegenerateStatus>>(
    `/sessions/${sessionId}/memory/regenerate`,
    request,
  );
  return res.data.data;
}

/**
 * 查询聚合文本重生成状态。
 * @param sessionId 会话 ID
 * @returns 当前重生成状态
 */
export async function getRegenerateStatus(
  sessionId: string,
): Promise<MemoryRegenerateStatus> {
  const res = await api.get<ApiResponse<MemoryRegenerateStatus>>(
    `/sessions/${sessionId}/memory/regenerate/status`,
  );
  return res.data.data;
}
