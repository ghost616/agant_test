import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockPost = vi.hoisted(() => vi.fn());

vi.mock('../api', () => ({
  default: {
    post: mockPost,
  },
}));

import { embed } from '../model';

describe('embed API', () => {
  beforeEach(() => {
    mockPost.mockReset();
  });

  it('应调用 POST /models/{id}/embed 并传递 EmbeddingRequest 请求体', async () => {
    const request = { input: 'hello world', model: 'embed-3' };
    mockPost.mockResolvedValueOnce({
      data: {
        data: {
          embeddings: [{ index: 0, embedding: [0.1, 0.2] }],
          usage: { promptTokens: 1, completionTokens: 0, totalTokens: 1 },
        },
      },
    });
    const result = await embed('model-1', request);
    expect(mockPost).toHaveBeenCalledWith('/models/model-1/embed', request);
    expect(result.embeddings).toEqual([{ index: 0, embedding: [0.1, 0.2] }]);
    expect(result.embeddings[0].embedding).toEqual([0.1, 0.2]);
    expect(result.usage).toEqual({ promptTokens: 1, completionTokens: 0, totalTokens: 1 });
  });

  it('应正确处理不同 id 与请求体', async () => {
    mockPost.mockResolvedValueOnce({ data: { data: { embeddings: [] } } });
    await embed('model-a', { input: 'x', model: 'm-1' });
    expect(mockPost).toHaveBeenCalledWith('/models/model-a/embed', { input: 'x', model: 'm-1' });

    mockPost.mockResolvedValueOnce({ data: { data: { embeddings: [{ index: 0, embedding: [0.5] }] } } });
    await embed('model-b', { input: 'y', model: 'm-2' });
    expect(mockPost).toHaveBeenCalledWith('/models/model-b/embed', { input: 'y', model: 'm-2' });
  });

  it('usage 字段可选，未返回时为 undefined', async () => {
    mockPost.mockResolvedValueOnce({ data: { data: { embeddings: [{ index: 0, embedding: [1] }] } } });
    const result = await embed('model-1', { input: 'x', model: 'm' });
    expect(result.usage).toBeUndefined();
  });

  it('应在 API 失败时抛出错误', async () => {
    mockPost.mockRejectedValueOnce(new Error('Network Error'));
    await expect(embed('model-1', { input: 'x', model: 'm' })).rejects.toThrow('Network Error');
  });
});
