import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('EvaluationResultList 导入与路由', () => {
  it('应导入 executeEvaluation', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('executeEvaluation');
  });

  it('应导入 getExecutionStatus', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('getExecutionStatus');
  });

  it('应导入 createEvalSession', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('createEvalSession');
  });

  it('应导入 generateEvalResult', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('generateEvalResult');
  });

  it('应导入 agentChatStream', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('agentChatStream');
  });

  it('应导入 executeTools 和 getToolStatus', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('executeTools');
    expect(source).toContain('getToolStatus');
  });

  it('应导入 continueChatStream', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('continueChatStream');
  });

  it('应使用 useNavigate 和 useParams', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('useNavigate');
    expect(source).toContain('useParams');
  });
});

describe('EvaluationResultList 执行逻辑', () => {
  it('BACKGROUND 模式应调 executeEvaluation 并轮询', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("executionType === 'BACKGROUND'");
    expect(source).toContain('executeEvaluation(evaluationId)');
    expect(source).toContain('pollExecutionStatus(evaluationId)');
  });

  it('BACKGROUND 模式轮询应检查 completed 和 error 状态', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("status.status === 'completed'");
    expect(source).toContain("status.status === 'error'");
  });

  it('FOREGROUND 模式应创建会话然后逐条发送消息', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('createEvalSession(evaluationId)');
    expect(source).toContain('evalSession.userMessages');
    expect(source).toContain('sendForegroundMessage');
  });

  it('FOREGROUND 模式最后应生成评估结果', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('generateEvalResult(evaluationId, evalSession.sessionId)');
  });

  it('应包含 sendForegroundMessage 函数，支持工具调用循环', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('sendForegroundMessage');
    expect(source).toContain('MAX_TOOL_LOOPS');
    expect(source).toContain('executeTools(sessionId)');
    expect(source).toContain('getToolStatus(');
    expect(source).toContain('continueChatStream(');
  });

  it('应包含停止执行按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('停止执行');
  });

  it('应包含回退按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('回退');
  });

  it('应包含执行按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('执行');
  });

  it('完成后应刷新数据列表 fetchData', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('fetchData');
  });
});

describe('EvaluationResultList runToolCycle', () => {
  it('应存在 runToolCycle 独立函数', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('runToolCycle');
  });

  it('runToolCycle 应接收 toolLoopCount 对象（非原始数字）', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('toolLoopCount: { current: number }');
  });

  it('runToolCycle 应在超出最大循环次数时返回', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('toolLoopCount.current >= maxToolLoops');
  });

  it('hasMore 分支中应更新 currentResult = nextExec', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('currentResult = nextExec');
  });

  it('runToolCycle 中 continueChatStream 的 onDone 应递归调用自身', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('await runToolCycle(sessionId, logLines, toolLoopCount, maxToolLoops)');
  });
});

describe('EvaluationResultList catch 异常分支', () => {
  it('catch 应检查 AbortError 并直接 return', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("err instanceof DOMException && err.name === 'AbortError'");
  });

  it('非取消异常应显示错误消息', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("message.error('执行失败: ' + (err instanceof Error ? err.message : String(err)))");
  });

  it('sendForegroundMessage 中 try-catch 应 reject 异常', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('await runToolCycle(sessionId, logLines, toolLoopCount, MAX_TOOL_LOOPS)');
    expect(source).toContain('reject(err)');
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
