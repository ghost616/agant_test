import type { ApiResponse, PageResult } from '../types/common';
import type { AgentLog, AgentLogQueryParams } from '../types/log';
import api from './api';

/**
 * 分页查询智能体日志。
 * @param params 查询参数（会话名/日志类型/日志等级/分页）
 * @returns 分页日志结果
 */
export async function listAgentLogs(
  params: AgentLogQueryParams,
): Promise<PageResult<AgentLog>> {
  const res = await api.get<ApiResponse<PageResult<AgentLog>>>('/agent-logs', {
    params,
  });
  return res.data.data;
}
