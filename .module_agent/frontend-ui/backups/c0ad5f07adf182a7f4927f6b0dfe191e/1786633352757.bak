import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    get: mockGet,
  },
}));

import { getSessionMemory } from '../memory';

describe('getSessionMemory', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /sessions/{sessionId}/memory 并携带 type/page/size 参数', async () => {
    mockGet.mockResolvedValueOnce({
      data: { data: { list: [], total: 0, page: 1, size: 20 } },
    });
    await getSessionMemory('100', 'DAILY', 1, 20);
    expect(mockGet).toHaveBeenCalledWith('/sessions/100/memory', {
      params: { type: 'DAILY', page: 1, size: 20 },
    });
  });

  it('不同 sessionId/type/page/size 参数正确透传', async () => {
    mockGet.mockResolvedValueOnce({
      data: { data: { list: [], total: 0, page: 1, size: 20 } },
    });
    await getSessionMemory('session-abc', 'GROUP', 3, 50);
    expect(mockGet).toHaveBeenCalledWith('/sessions/session-abc/memory', {
      params: { type: 'GROUP', page: 3, size: 50 },
    });
  });

  it('应返回 PageResult<SessionMemoryDocument> 结构（list/total/page/size）', async () => {
    const fakeResult = {
      list: [
        {
          sessionId: '100',
          aggregationType: 'DAILY',
          aggregationStartSeq: 1,
          aggregationEndSeq: 3,
          aggregationStartTime: 1720000000000,
          aggregationEndTime: 1720000000000,
          aggregationText: '摘要',
        },
      ],
      total: 1,
      page: 1,
      size: 20,
    };
    mockGet.mockResolvedValueOnce({ data: { data: fakeResult } });
    const result = await getSessionMemory('100', 'DAILY', 1, 20);
    expect(result).toEqual(fakeResult);
    expect(result.list).toHaveLength(1);
    expect(result.total).toBe(1);
    expect(result.page).toBe(1);
    expect(result.size).toBe(20);
  });

  it('空列表时返回空的 PageResult', async () => {
    mockGet.mockResolvedValueOnce({
      data: { data: { list: [], total: 0, page: 1, size: 20 } },
    });
    const result = await getSessionMemory('100', 'GROUP', 1, 20);
    expect(result.list).toEqual([]);
    expect(result.total).toBe(0);
  });

  it('应在 API 失败时抛出错误', async () => {
    const testError = new Error('Network Error');
    mockGet.mockRejectedValueOnce(testError);
    await expect(getSessionMemory('100', 'DAILY', 1, 20)).rejects.toThrow('Network Error');
  });
});
