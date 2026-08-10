import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

const mockPost = vi.hoisted(() => vi.fn());
const mockGet = vi.hoisted(() => vi.fn());
const mockPut = vi.hoisted(() => vi.fn());
const mockDelete = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    post: mockPost,
    get: mockGet,
    put: mockPut,
    delete: mockDelete,
  },
}));

import {
  executeEvaluation,
  createEvalSession,
  generateEvalResult,
  getGenerateStatus,
  getEvaluationResult,
  deleteEvaluationResult,
  batchDeleteEvaluationResults,
  clearEvaluationResults,
  getEvaluationCacheStatus,
  getEvaluationStream,
} from '../evaluation';

describe('executeEvaluation', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /evaluations/{id}/execute 并返回 executionSessionId', async () => {
    const fakeStatus = { evaluationId: 'eval-123', executionSessionId: 'exec-1' };
    mockPost.mockResolvedValueOnce({ data: { data: fakeStatus } });
    const result = await executeEvaluation('eval-123');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/eval-123/execute');
    expect(result.executionSessionId).toBe('exec-1');
  });

  it('应正确处理不同 id', async () => {
    mockPost.mockResolvedValueOnce({ data: { data: { evaluationId: 'id-a', executionSessionId: 'exec-a' } } });
    await executeEvaluation('id-a');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/id-a/execute');

    mockPost.mockResolvedValueOnce({ data: { data: { evaluationId: 'id-b', executionSessionId: 'exec-b' } } });
    await executeEvaluation('id-b');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/id-b/execute');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(executeEvaluation('eval-123')).rejects.toThrow('Network Error');
  });
});

describe('getEvaluationCacheStatus', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /evaluations/session/{executionSessionId}/cache/status 并返回 hasCache', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: { hasCache: true } } });
    const result = await getEvaluationCacheStatus('exec-1');
    expect(mockGet).toHaveBeenCalledWith('/evaluations/session/exec-1/cache/status');
    expect(result.hasCache).toBe(true);
  });

  it('应返回 hasCache 为 false 的响应', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: { hasCache: false } } });
    const result = await getEvaluationCacheStatus('exec-2');
    expect(result.hasCache).toBe(false);
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(getEvaluationCacheStatus('exec-1')).rejects.toThrow('Network Error');
  });
});

describe('getEvaluationStream', () => {
  const originalFetch = globalThis.fetch;

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('应连接 /api/evaluations/session/{executionSessionId}/stream 并复用 processSSEStream', async () => {
    const mockResponse = {
      ok: true,
      status: 200,
      body: {
        getReader: (): { read: () => Promise<{ done: boolean; value?: Uint8Array }> } => ({
          read: () => Promise.resolve({ done: true }),
        }),
      },
    } as unknown as Response;
    const mockFetch = vi.fn().mockResolvedValue(mockResponse);
    globalThis.fetch = mockFetch as typeof fetch;

    const onDelta = vi.fn();
    const onDone = vi.fn();
    const controller = getEvaluationStream('exec-1', {
      onDelta,
      onReasoning: () => {},
      onDone,
      onError: () => {},
    });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/evaluations/session/exec-1/stream',
      expect.objectContaining({ method: 'GET', signal: controller.signal }),
    );

    await new Promise((r) => setTimeout(r, 0));
    expect(onDone).toHaveBeenCalledWith(false);
  });
});

describe('createEvalSession', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /evaluations/{id}/session 并返回会话信息', async () => {
    const fakeSession = { sessionId: 'session-1', userMessages: ['msg1'] };
    mockPost.mockResolvedValueOnce({ data: { data: fakeSession } });
    const result = await createEvalSession('eval-123');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/eval-123/session');
    expect(result).toEqual(fakeSession);
  });

  it('应返回多条用户消息', async () => {
    const fakeSession = {
      sessionId: 'session-2',
      userMessages: ['msg1', 'msg2', 'msg3'],
    };
    mockPost.mockResolvedValueOnce({ data: { data: fakeSession } });
    const result = await createEvalSession('eval-456');
    expect(result.userMessages).toHaveLength(3);
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(createEvalSession('eval-123')).rejects.toThrow('Network Error');
  });
});

describe('generateEvalResult', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /evaluations/{id}/session/{sessionId}/generate 并返回 Promise<void>', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await generateEvalResult('eval-123', 'session-456');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/eval-123/session/session-456/generate');
  });

  it('应正确处理不同参数组合', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await generateEvalResult('eval-a', 'session-b');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/eval-a/session/session-b/generate');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(generateEvalResult('eval-123', 'session-456')).rejects.toThrow('Network Error');
  });
});

describe('getGenerateStatus', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /evaluations/{id}/session/{sessionId}/generate/status 并返回状态', async () => {
    const fakeStatus = { status: 'running', currentStep: 1, totalSteps: 5 };
    mockGet.mockResolvedValueOnce({ data: { data: fakeStatus } });
    const result = await getGenerateStatus('eval-123', 'session-456');
    expect(mockGet).toHaveBeenCalledWith('/evaluations/eval-123/session/session-456/generate/status');
    expect(result).toEqual(fakeStatus);
  });

  it('应返回 completed 状态', async () => {
    const fakeStatus = { status: 'completed', currentStep: 5, totalSteps: 5 };
    mockGet.mockResolvedValueOnce({ data: { data: fakeStatus } });
    const result = await getGenerateStatus('eval-123', 'session-456');
    expect(result.status).toBe('completed');
  });

  it('应返回 failed 状态', async () => {
    const fakeStatus = { status: 'failed', currentStep: 3, totalSteps: 5 };
    mockGet.mockResolvedValueOnce({ data: { data: fakeStatus } });
    const result = await getGenerateStatus('eval-123', 'session-456');
    expect(result.status).toBe('failed');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(getGenerateStatus('eval-123', 'session-456')).rejects.toThrow('Network Error');
  });
});

describe('getEvaluationResult', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /evaluations/results/{resultId} 并返回评估结果', async () => {
    const fakeResult = { id: 'result-1', evaluationId: 'eval-1', evaluationSessionId: 'session-1', result: '测试结果', totalTokenUsed: '1000' };
    mockGet.mockResolvedValueOnce({ data: { data: fakeResult } });
    const result = await getEvaluationResult('result-1');
    expect(mockGet).toHaveBeenCalledWith('/evaluations/results/result-1');
    expect(result).toEqual(fakeResult);
  });

  it('应正确处理不同 resultId', async () => {
    const fakeResult = { id: 'result-2', evaluationId: 'eval-2', evaluationSessionId: 'session-2' };
    mockGet.mockResolvedValueOnce({ data: { data: fakeResult } });
    const result = await getEvaluationResult('result-2');
    expect(result.id).toBe('result-2');
  });

  it('应处理无 result 字段的情况', async () => {
    const fakeResult = { id: 'result-3', evaluationId: 'eval-3', evaluationSessionId: 'session-3' };
    mockGet.mockResolvedValueOnce({ data: { data: fakeResult } });
    const result = await getEvaluationResult('result-3');
    expect(result.result).toBeUndefined();
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(getEvaluationResult('result-1')).rejects.toThrow('Network Error');
  });
});

describe('deleteEvaluationResult', () => {
  beforeEach(() => {
    mockDelete.mockReset();
  });

  it('应调用 DELETE /evaluations/results/{id} 并返回 Promise<void>', async () => {
    mockDelete.mockResolvedValueOnce(undefined);
    await deleteEvaluationResult('result-1');
    expect(mockDelete).toHaveBeenCalledWith('/evaluations/results/result-1');
  });

  it('应正确处理不同 id 参数', async () => {
    mockDelete.mockResolvedValueOnce(undefined);
    await deleteEvaluationResult('id-a');
    expect(mockDelete).toHaveBeenCalledWith('/evaluations/results/id-a');

    mockDelete.mockResolvedValueOnce(undefined);
    await deleteEvaluationResult('id-b');
    expect(mockDelete).toHaveBeenCalledWith('/evaluations/results/id-b');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockDelete.mockRejectedValueOnce(new Error('Network Error'));
    await expect(deleteEvaluationResult('result-1')).rejects.toThrow('Network Error');
  });
});

describe('batchDeleteEvaluationResults', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /evaluations/results/batch-delete 并传 ids 裸数组', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await batchDeleteEvaluationResults(['result-1', 'result-2']);
    expect(mockPost).toHaveBeenCalledWith('/evaluations/results/batch-delete', [
      'result-1',
      'result-2',
    ]);
  });

  it('应正确传递单个 id 的数组', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await batchDeleteEvaluationResults(['result-a']);
    expect(mockPost).toHaveBeenCalledWith('/evaluations/results/batch-delete', [
      'result-a',
    ]);
  });

  it('应支持空数组传入', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await batchDeleteEvaluationResults([]);
    expect(mockPost).toHaveBeenCalledWith('/evaluations/results/batch-delete', []);
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(batchDeleteEvaluationResults(['result-1'])).rejects.toThrow('Network Error');
  });
});

describe('clearEvaluationResults', () => {
  beforeEach(() => {
    mockDelete.mockReset();
  });

  it('应调用 DELETE /evaluations/{evaluationId}/results 并返回 Promise<void>', async () => {
    mockDelete.mockResolvedValueOnce(undefined);
    await clearEvaluationResults('eval-123');
    expect(mockDelete).toHaveBeenCalledWith('/evaluations/eval-123/results');
  });

  it('应正确处理不同 evaluationId', async () => {
    mockDelete.mockResolvedValueOnce(undefined);
    await clearEvaluationResults('eval-a');
    expect(mockDelete).toHaveBeenCalledWith('/evaluations/eval-a/results');

    mockDelete.mockResolvedValueOnce(undefined);
    await clearEvaluationResults('eval-b');
    expect(mockDelete).toHaveBeenCalledWith('/evaluations/eval-b/results');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockDelete.mockRejectedValueOnce(new Error('Network Error'));
    await expect(clearEvaluationResults('eval-123')).rejects.toThrow('Network Error');
  });
});
