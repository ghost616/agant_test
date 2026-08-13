import type { ApiResponse, PageResult } from '../types/common';
import type {
  MemoryAggregationType,
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
