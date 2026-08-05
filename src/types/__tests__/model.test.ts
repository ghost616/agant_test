import { describe, it, expect, expectTypeOf } from 'vitest';
import type {
  EmbeddingRequest,
  EmbeddingResponse,
  UsageInfo,
} from '../model';

describe('EmbeddingRequest 类型', () => {
  it('应包含 input: string 与 model: string 字段', () => {
    expectTypeOf<EmbeddingRequest>().toHaveProperty('input').toEqualTypeOf<string>();
    expectTypeOf<EmbeddingRequest>().toHaveProperty('model').toEqualTypeOf<string>();
  });

  it('合法对象可通过类型检查', () => {
    const req: EmbeddingRequest = { input: '文本', model: 'embed-3' };
    expect(req.input).toBe('文本');
    expect(req.model).toBe('embed-3');
  });
});

describe('EmbeddingResponse 类型', () => {
  it('应包含 embeddings: EmbeddingItem[] 与可选 usage 字段', () => {
    expectTypeOf<EmbeddingResponse>().toHaveProperty('embeddings').toEqualTypeOf<
      Array<{ index: number; embedding: number[] }>
    >();
    expectTypeOf<EmbeddingResponse>().toHaveProperty('usage').toEqualTypeOf<UsageInfo | undefined>();
  });

  it('usage 可选：不带 usage 的对象可通过类型检查', () => {
    const res: EmbeddingResponse = { embeddings: [{ index: 0, embedding: [0.1, 0.2] }] };
    expect(res.embeddings[0].embedding).toEqual([0.1, 0.2]);
    expect(res.usage).toBeUndefined();
  });

  it('带 usage 的对象可通过类型检查', () => {
    const usage: UsageInfo = { promptTokens: 10, completionTokens: 5, totalTokens: 15 };
    const res: EmbeddingResponse = { embeddings: [{ index: 0, embedding: [0.1] }], usage };
    expect(res.usage?.totalTokens).toBe(15);
  });
});
