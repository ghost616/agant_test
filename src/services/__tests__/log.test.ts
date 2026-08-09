import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    get: mockGet,
  },
}));

import { listAgentLogs } from '../log';

function okResponse(data: unknown) {
  return {
    data: { code: 'SYS-000', success: true, message: '操作成功', data },
  };
}

describe('listAgentLogs', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /agent-logs 并传递全部查询参数', async () => {
    const pageResult = { list: [], total: 0, page: 2, size: 50 };
    mockGet.mockResolvedValueOnce(okResponse(pageResult));

    const result = await listAgentLogs({
      sessionName: '会话A',
      logType: 'MODEL_CALL',
      logLevel: 'INFO',
      page: 2,
      size: 50,
    });

    expect(mockGet).toHaveBeenCalledWith('/agent-logs', {
      params: {
        sessionName: '会话A',
        logType: 'MODEL_CALL',
        logLevel: 'INFO',
        page: 2,
        size: 50,
      },
    });
    expect(result).toEqual(pageResult);
  });

  it('可选筛选参数缺省时不传递', async () => {
    mockGet.mockResolvedValueOnce(okResponse({ list: [], total: 0, page: 1, size: 20 }));

    await listAgentLogs({ page: 1, size: 20 });

    expect(mockGet).toHaveBeenCalledWith('/agent-logs', {
      params: { page: 1, size: 20 },
    });
  });

  it('应返回 PageResult 结构（list/total/page/size）', async () => {
    const list = [
      {
        id: '1',
        sessionName: '会话A',
        logType: 'ROUTE',
        logLevel: 'INFO',
        logData: '{}',
        createTime: '2026-08-09 10:00:00',
      },
    ];
    mockGet.mockResolvedValueOnce(okResponse({ list, total: 1, page: 1, size: 20 }));

    const result = await listAgentLogs({ page: 1, size: 20 });

    expect(result.list).toHaveLength(1);
    expect(result.total).toBe(1);
    expect(result.page).toBe(1);
    expect(result.size).toBe(20);
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));

    await expect(listAgentLogs({ page: 1, size: 20 })).rejects.toThrow('Network Error');
  });
});
