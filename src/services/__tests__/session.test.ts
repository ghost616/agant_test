import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockPost = vi.hoisted(() => vi.fn());
const mockGet = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    post: mockPost,
    get: mockGet,
  },
}));

import { stopChat, listChildSessions, getSubSessionData, completeSubSession } from '../session';

describe('stopChat', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /chat/${sessionId}/stop 并返回 Promise<void>', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    const sessionId = 'test-session-123';
    await stopChat(sessionId);
    expect(mockPost).toHaveBeenCalledWith(`/chat/${sessionId}/stop`);
  });

  it('应在不同 sessionId 下正确拼接 URL', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await stopChat('session-a');
    expect(mockPost).toHaveBeenCalledWith('/chat/session-a/stop');

    mockPost.mockResolvedValueOnce(undefined);
    await stopChat('session-b');
    expect(mockPost).toHaveBeenCalledWith('/chat/session-b/stop');
  });

  it('应在 API 失败时抛出错误', async () => {
    const testError = new Error('Network Error');
    mockPost.mockRejectedValueOnce(testError);
    await expect(stopChat('test-session')).rejects.toThrow('Network Error');
  });
});

describe('listChildSessions', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /sessions/{parentId}/children', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: [] } });
    await listChildSessions('parent-123');
    expect(mockGet).toHaveBeenCalledWith('/sessions/parent-123/children');
  });

  it('应在不同 parentId 下正确拼接 URL', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: [] } });
    await listChildSessions('parent-a');
    expect(mockGet).toHaveBeenCalledWith('/sessions/parent-a/children');

    mockGet.mockResolvedValueOnce({ data: { data: [] } });
    await listChildSessions('parent-b');
    expect(mockGet).toHaveBeenCalledWith('/sessions/parent-b/children');
  });

  it('应返回 Session 数组', async () => {
    const fakeSessions = [
      { id: 'child-1', title: '子会话1', agentId: 'a', modelId: 'm', createTime: '', updateTime: '' },
    ];
    mockGet.mockResolvedValueOnce({ data: { data: fakeSessions } });
    const result = await listChildSessions('parent-123');
    expect(result).toEqual(fakeSessions);
  });

  it('应在 API 失败时抛出错误', async () => {
    const testError = new Error('Network Error');
    mockGet.mockRejectedValueOnce(testError);
    await expect(listChildSessions('parent-123')).rejects.toThrow('Network Error');
  });
});

describe('getSubSessionData', () => {
  beforeEach(() => {
    mockGet.mockReset();
  });

  it('应调用 GET /sessions/{sessionId}/sub-session-data', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: null } });
    await getSubSessionData('session-123');
    expect(mockGet).toHaveBeenCalledWith('/sessions/session-123/sub-session-data');
  });

  it('应在存在子会话数据时返回 SubSessionData', async () => {
    const fakeData = { childSessionId: 'child-1', userMessage: '子会话请求' };
    mockGet.mockResolvedValueOnce({ data: { data: fakeData } });
    const result = await getSubSessionData('session-123');
    expect(result).toEqual(fakeData);
  });

  it('应在无子会话数据时返回 null', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: null } });
    const result = await getSubSessionData('session-123');
    expect(result).toBeNull();
  });

  it('应正确处理不同 sessionId 的 URL 拼接', async () => {
    mockGet.mockResolvedValueOnce({ data: { data: null } });
    await getSubSessionData('session-a');
    expect(mockGet).toHaveBeenCalledWith('/sessions/session-a/sub-session-data');

    mockGet.mockResolvedValueOnce({ data: { data: null } });
    await getSubSessionData('session-b');
    expect(mockGet).toHaveBeenCalledWith('/sessions/session-b/sub-session-data');
  });

  it('应返回包含 thinking 字段的 SubSessionData（thinking=true）', async () => {
    const fakeData = { childSessionId: 'child-1', userMessage: '子会话请求', thinking: true };
    mockGet.mockResolvedValueOnce({ data: { data: fakeData } });
    const result = await getSubSessionData('session-123');
    expect(result).toEqual(fakeData);
    expect(result!.thinking).toBe(true);
  });

  it('应返回包含 thinking 字段的 SubSessionData（thinking=false）', async () => {
    const fakeData = { childSessionId: 'child-2', userMessage: '子会话请求2', thinking: false };
    mockGet.mockResolvedValueOnce({ data: { data: fakeData } });
    const result = await getSubSessionData('session-456');
    expect(result).toEqual(fakeData);
    expect(result!.thinking).toBe(false);
  });

  it('应返回不含 thinking 字段的 SubSessionData（thinking 可选）', async () => {
    const fakeData = { childSessionId: 'child-3', userMessage: '子会话请求3' };
    mockGet.mockResolvedValueOnce({ data: { data: fakeData } });
    const result = await getSubSessionData('session-789');
    expect(result).toEqual(fakeData);
    expect(result!.thinking).toBeUndefined();
  });

  it('应在 API 失败时抛出错误', async () => {
    const testError = new Error('Network Error');
    mockGet.mockRejectedValueOnce(testError);
    await expect(getSubSessionData('session-123')).rejects.toThrow('Network Error');
  });
});

describe('completeSubSession', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /sessions/{sessionId}/complete-sub-session 并返回 Promise<void>', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await completeSubSession('session-123');
    expect(mockPost).toHaveBeenCalledWith('/sessions/session-123/complete-sub-session');
  });

  it('应正确处理不同 sessionId 的 URL 拼接', async () => {
    mockPost.mockResolvedValueOnce(undefined);
    await completeSubSession('session-a');
    expect(mockPost).toHaveBeenCalledWith('/sessions/session-a/complete-sub-session');

    mockPost.mockResolvedValueOnce(undefined);
    await completeSubSession('session-b');
    expect(mockPost).toHaveBeenCalledWith('/sessions/session-b/complete-sub-session');
  });

  it('应在 API 失败时抛出错误', async () => {
    const testError = new Error('Network Error');
    mockPost.mockRejectedValueOnce(testError);
    await expect(completeSubSession('session-123')).rejects.toThrow('Network Error');
  });
});
