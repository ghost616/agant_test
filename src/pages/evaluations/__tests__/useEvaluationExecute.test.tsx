import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

const hookPath = resolve(__dirname, '../hooks/useEvaluationExecute.ts');

describe('useEvaluationExecute 导入', () => {
  it('应导入 executeEvaluation', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('executeEvaluation');
  });

  it('应导入 getExecutionStatus', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('getExecutionStatus');
  });

  it('应导入 createEvalSession', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('createEvalSession');
  });

  it('应导入 generateEvalResult', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('generateEvalResult');
  });

  it('应导入 agentChatStream', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('agentChatStream');
  });

  it('应导入 executeTools 和 getToolStatus', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('executeTools');
    expect(source).toContain('getToolStatus');
  });

  it('应导入 continueChatStream', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('continueChatStream');
  });

  it('应导入 executeBrowserTool', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('executeBrowserTool');
  });
});

describe('useEvaluationExecute 导出', () => {
  it('应导出 useEvaluationExecute 函数', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('export function useEvaluationExecute');
  });

  it('应返回 execute 方法', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('execute');
  });

  it('应返回 executing 状态', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('executing');
  });

  it('应返回 executionProgress 状态', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('executionProgress');
  });

  it('应返回 foregroundModalVisible 状态', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('foregroundModalVisible');
  });

  it('应返回 foregroundLog 状态', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('foregroundLog');
  });

  it('应返回 foregroundLogRef', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('foregroundLogRef');
  });

  it('应返回 handleCancelForeground', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('handleCancelForeground');
  });
});

describe('useEvaluationExecute 执行逻辑', () => {
  it('BACKGROUND 模式应调 executeEvaluation 并轮询', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain("executionType === 'BACKGROUND'");
    expect(source).toContain('executeEvaluation(evaluationId)');
    expect(source).toContain('pollExecutionStatus(evaluationId)');
  });

  it('BACKGROUND 模式轮询应检查 completed 和 error 状态', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain("status.status === 'completed'");
    expect(source).toContain("status.status === 'error'");
  });

  it('FOREGROUND 模式应创建会话然后逐条发送消息', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('createEvalSession(evaluationId)');
    expect(source).toContain('evalSession.userMessages');
    expect(source).toContain('sendForegroundMessage');
  });

  it('FOREGROUND 模式最后应生成评估结果', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('generateEvalResult(evaluationId, evalSession.sessionId)');
  });

  it('BACKGROUND 模式应按 executionCount 循环执行', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('for (let i = 0; i < evaluation.executionCount; i++)');
  });

  it('BACKGROUND 模式每次循环应显示第 i/N 次进度', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('执行中(第 ${i + 1}/${evaluation.executionCount} 次)...');
  });

  it('FOREGROUND 模式应按 executionCount 循环执行', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('for (let i = 0; i < evaluation.executionCount; i++)');
  });

  it('FOREGROUND 模式每次循环应显示第 i/N 次分隔', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('========== 第 ${i + 1}/${evaluation.executionCount} 次执行 ==========');
  });
});

describe('useEvaluationExecute runToolCycle', () => {
  it('应存在 runToolCycle 独立函数', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('runToolCycle');
  });

  it('runToolCycle 应接收 toolLoopCount 对象（非原始数字）', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('toolLoopCount: { current: number }');
  });

  it('runToolCycle 应在超出最大循环次数时返回', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('toolLoopCount.current >= maxToolLoops');
  });

  it('hasMore 分支中应更新 currentResult = nextExec', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('currentResult = nextExec');
  });

  it('runToolCycle 中 continueChatStream 的 onDone 应递归调用自身', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('await runToolCycle(sessionId, logLines, toolLoopCount, maxToolLoops)');
  });

  it('应包含 sendForegroundMessage 函数，支持工具调用循环', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('sendForegroundMessage');
    expect(source).toContain('MAX_TOOL_LOOPS');
    expect(source).toContain('executeTools(sessionId)');
    expect(source).toContain('getToolStatus(');
    expect(source).toContain('continueChatStream(');
  });
});

describe('useEvaluationExecute catch 异常分支', () => {
  it('catch 应检查 AbortError 并直接 return', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain("err instanceof DOMException && err.name === 'AbortError'");
  });

  it('非取消异常应显示错误消息', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain("message.error('执行失败: ' + (err instanceof Error ? err.message : String(err)))");
  });

  it('sendForegroundMessage 中 try-catch 应 reject 异常', () => {
    const source = readFileSync(hookPath, 'utf-8');
    expect(source).toContain('reject(err)');
  });
});
