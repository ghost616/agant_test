import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('EvaluationList 按钮文字', () => {
  it('操作列的基准会话按钮文字应为"基准会话"（已缩短）', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('基准会话');
    expect(source).not.toContain('基准会话数据产生');
  });

  it('应包含新增评估按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('新增评估');
  });

  it('应包含修改按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('修改');
  });

  it('应包含进行评估按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('进行评估');
  });

  it('应包含删除按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('删除');
  });

  it('应包含返回按钮（带 urlAgentEvalId 时）', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('返回');
  });
});

describe('EvaluationList 导入与路由', () => {
  it('应导入 useNavigate', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('useNavigate');
  });

  it('应导入 useParams', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('useParams');
  });

  it('导航到 evaluations 列表页应使用 navigate("/evaluations")', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain("navigate('/evaluations')");
  });

  it('导航到评估结果页应使用 evaluations/items/${id}/results', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('/evaluations/items/${record.id}/results');
  });

  it('导航到基准会话应使用 sessions/${id}/chat?benchmark=1', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('/sessions/${record.benchmarkSessionId}/chat?benchmark=1');
  });
});

describe('EvaluationList 基准会话条件渲染', () => {
  it('benchmarkSessionId 存在时才显示基准会话按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('record.benchmarkSessionId &&');
    expect(source).toContain('基准会话');
  });
});
