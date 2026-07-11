import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockPost = vi.hoisted(() => vi.fn());
const mockGet = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    post: mockPost,
    get: mockGet,
  },
}));

import { stopChat, listChildSessions } from '../session';

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
