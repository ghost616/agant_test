import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockPost = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    post: mockPost,
  },
}));

import { stopChat } from '../session';

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
