import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('EvaluationResultDetail 导入与路由', () => {
  it('应导入 getEvaluationResult', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('getEvaluationResult');
  });

  it('应导入 getSessionMessages', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('getSessionMessages');
  });

  it('应使用 useNavigate 和 useParams', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('useNavigate');
    expect(source).toContain('useParams');
  });

  it('应使用 resultId 参数获取评估结果', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('resultId');
  });

  it('应使用 ReactMarkdown 渲染消息', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('ReactMarkdown');
  });
});

describe('EvaluationResultDetail 加载与状态', () => {
  it('加载中应显示 Spin 和提示文字', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('Spin');
    expect(source).toContain('加载中...');
  });

  it('结果为空时应显示暂无评估结果', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('暂无评估结果');
  });

  it('消息为空时应显示暂无会话历史消息', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('暂无会话历史消息');
  });
});

describe('EvaluationResultDetail 消息渲染', () => {
  it('应渲染 user/assistant/tool/system 四种角色', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('user');
    expect(source).toContain('assistant');
    expect(source).toContain('tool');
    expect(source).toContain('system');
  });

  it('角色配置应包含 label 和 color', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('label');
    expect(source).toContain('color');
  });

  it('用户消息应靠右显示', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain("isUser ? 'flex-end' : 'flex-start'");
  });

  it('工具消息应解析 toolResult JSON', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('JSON.parse(msg.toolResult)');
    expect(source).toContain('toolName');
    expect(source).toContain('arguments');
  });
});

describe('EvaluationResultDetail 导航', () => {
  it('应包含返回按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('返回');
  });

  it('返回按钮应导航到结果列表页或评估列表页', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultDetail.tsx'), 'utf-8');
    expect(source).toContain('/evaluations/items/${evaluationId}/results');
    expect(source).toContain("navigate('/evaluations')");
  });
});
