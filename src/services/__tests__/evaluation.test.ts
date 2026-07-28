import { describe, it, expect, vi, beforeEach } from 'vitest';

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
  getExecutionStatus,
  createEvalSession,
  generateEvalResult,
} from '../evaluation';

describe('executeEvaluation', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /evaluations/{id}/execute 并返回 Promise<void>', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await executeEvaluation('eval-123');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/eval-123/execute');
  });

  it('应正确处理不同 id', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await executeEvaluation('id-a');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/id-a/execute');

    mockPost.mockResolvedValueOnce(undefined);
    await executeEvaluation('id-b');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/id-b/execute');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(executeEvaluation('eval-123')).rejects.toThrow('Network Error');
  });
});

describe('getExecutionStatus', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /evaluations/{id}/status 并返回状态', async () => {
    const fakeStatus = { status: 'running', currentStep: 1, totalSteps: 5 };
    mockGet.mockResolvedValueOnce({ data: { data: fakeStatus } });
    const result = await getExecutionStatus('eval-123');
    expect(mockGet).toHaveBeenCalledWith('/evaluations/eval-123/status');
    expect(result).toEqual(fakeStatus);
  });

  it('应返回 completed 状态', async () => {
    const fakeStatus = { status: 'completed', currentStep: 5, totalSteps: 5 };
    mockGet.mockResolvedValueOnce({ data: { data: fakeStatus } });
    const result = await getExecutionStatus('eval-123');
    expect(result.status).toBe('completed');
    expect(result.currentStep).toBe(5);
    expect(result.totalSteps).toBe(5);
  });

  it('应返回 error 状态', async () => {
    const fakeStatus = { status: 'error', currentStep: 3, totalSteps: 5 };
    mockGet.mockResolvedValueOnce({ data: { data: fakeStatus } });
    const result = await getExecutionStatus('eval-123');
    expect(result.status).toBe('error');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockGet.mockRejectedValueOnce(new Error('Network Error'));
    await expect(getExecutionStatus('eval-123')).rejects.toThrow('Network Error');
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

  it('应调用 POST /evaluations/{id}/results/{sessionId} 并返回 Promise<void>', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await generateEvalResult('eval-123', 'session-456');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/eval-123/results/session-456');
  });

  it('应正确处理不同参数组合', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await generateEvalResult('eval-a', 'session-b');
    expect(mockPost).toHaveBeenCalledWith('/evaluations/eval-a/results/session-b');
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(generateEvalResult('eval-123', 'session-456')).rejects.toThrow('Network Error');
  });
});
