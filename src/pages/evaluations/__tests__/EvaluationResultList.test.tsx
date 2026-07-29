import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('EvaluationResultList 导入与路由', () => {
  it('应使用 useNavigate 和 useParams', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('useNavigate');
    expect(source).toContain('useParams');
  });
});

describe('EvaluationResultList 执行逻辑', () => {
  it('应包含停止执行按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('停止执行');
  });

  it('应包含返回按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('返回');
  });

  it('应包含执行按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('执行');
  });

  it('完成后应刷新数据列表 fetchData', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('fetchData');
  });

  it('应集成 useEvaluationExecute hook', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('useEvaluationExecute');
    expect(source).toContain("from './hooks/useEvaluationExecute'");
  });

  it('handleExecute 应调用 execute(evaluationId, evaluation, fetchData)', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('handleExecute');
    expect(source).toContain('execute(evaluationId, evaluation, fetchData)');
  });
});

describe('EvaluationResultList 表格', () => {
  it('应包含 ID 列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("title: 'ID'");
  });

  it('应包含会话 ID 列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('会话ID');
  });

  it('应包含 Token 消耗列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('Token消耗');
  });

  it('应包含创建时间列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('创建时间');
  });

  it('应包含操作列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("key: 'actions'");
  });

  it('操作列应包含查看结果按钮并导航到详情页', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('查看结果');
    expect(source).toContain('navigate(`/evaluations/results/${record.id}`)');
  });

  it('应包含执行进度显示', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('executionProgress');
  });
});
