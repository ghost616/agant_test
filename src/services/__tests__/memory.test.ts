import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGet = vi.hoisted(() => vi.fn());
const mockPut = vi.hoisted(() => vi.fn());
const mockPost = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    get: mockGet,
    put: mockPut,
    post: mockPost,
  },
}));

import {
  getSessionMemory,
  getMemoryPrompt,
  saveMemoryPrompt,
  updateMemoryDocument,
  regenerateMemory,
  getRegenerateStatus,
} from '../memory';

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

describe('getMemoryPrompt', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /sessions/{id}/memory-prompt 并返回提示语', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: '自定义提示语' } });
    const result = await getMemoryPrompt('100');
    expect(mockGet).toHaveBeenCalledWith('/sessions/100/memory-prompt');
    expect(result).toBe('自定义提示语');
  });

  it('未配置时返回空字符串', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: '' } });
    const result = await getMemoryPrompt('100');
    expect(result).toBe('');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(getMemoryPrompt('100')).rejects.toThrow('Network Error');
  });
});

describe('saveMemoryPrompt', () => {
  beforeEach(() => {
    mockPut.mockReset();
  });

  it('应调用 PUT /sessions/{id}/memory-prompt 并携带 prompt 请求体', async () => {
    mockPut.mockResolvedValueOnce({ data: { data: null } });
    await saveMemoryPrompt('100', '新的提示语');
    expect(mockPut).toHaveBeenCalledWith('/sessions/100/memory-prompt', {
      prompt: '新的提示语',
    });
  });

  it('不同 sessionId/prompt 正确透传', async () => {
    mockPut.mockResolvedValueOnce({ data: { data: null } });
    await saveMemoryPrompt('session-b', 'A');
    expect(mockPut).toHaveBeenCalledWith('/sessions/session-b/memory-prompt', {
      prompt: 'A',
    });
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPut.mockRejectedValueOnce(new Error('Network Error'));
    await expect(saveMemoryPrompt('100', 'p')).rejects.toThrow('Network Error');
  });
});

describe('updateMemoryDocument', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /sessions/{id}/memory/update 并携带 docId/text 请求体', async () => {
    mockPost.mockResolvedValueOnce({ data: { data: null } });
    await updateMemoryDocument('100', '100_DAILY_1_5', '新的聚合文本');
    expect(mockPost).toHaveBeenCalledWith('/sessions/100/memory/update', {
      docId: '100_DAILY_1_5',
      text: '新的聚合文本',
    });
  });

  it('不同 docId/text 正确透传', async () => {
    mockPost.mockResolvedValueOnce({ data: { data: null } });
    await updateMemoryDocument('session-b', 'doc-2', 'text-2');
    expect(mockPost).toHaveBeenCalledWith('/sessions/session-b/memory/update', {
      docId: 'doc-2',
      text: 'text-2',
    });
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(updateMemoryDocument('100', 'd', 't')).rejects.toThrow('Network Error');
  });
});

describe('regenerateMemory', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /sessions/{id}/memory/regenerate 并携带请求体', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        data: {
          sessionId: '100',
          docId: '100_DAILY_1_5',
          status: 'RUNNING',
        },
      },
    });
    const result = await regenerateMemory('100', {
      docId: '100_DAILY_1_5',
      startSeq: 1,
      endSeq: 5,
      prompt: '提示语',
    });
    expect(mockPost).toHaveBeenCalledWith('/sessions/100/memory/regenerate', {
      docId: '100_DAILY_1_5',
      startSeq: 1,
      endSeq: 5,
      prompt: '提示语',
    });
    expect(result.status).toBe('RUNNING');
  });

  it('prompt 为空时请求体不含 prompt 字段', async () => {
    mockPost.mockResolvedValueOnce({
      data: { data: { sessionId: '100', docId: 'd', status: 'RUNNING' } },
    });
    await regenerateMemory('100', { docId: 'd', startSeq: 1, endSeq: 5 });
    expect(mockPost).toHaveBeenCalledWith('/sessions/100/memory/regenerate', {
      docId: 'd',
      startSeq: 1,
      endSeq: 5,
    });
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(
      regenerateMemory('100', { docId: 'd' }),
    ).rejects.toThrow('Network Error');
  });
});

describe('getRegenerateStatus', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /sessions/{id}/memory/regenerate/status', async () => {
    mockGet.mockResolvedValueOnce({
      data: { data: { sessionId: '100', status: 'COMPLETED', aggregationText: '新文本' } },
    });
    const result = await getRegenerateStatus('100');
    expect(mockGet).toHaveBeenCalledWith('/sessions/100/memory/regenerate/status');
    expect(result.status).toBe('COMPLETED');
    expect(result.aggregationText).toBe('新文本');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(getRegenerateStatus('100')).rejects.toThrow('Network Error');
  });
});
