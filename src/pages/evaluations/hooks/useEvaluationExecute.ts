import { useCallback, useEffect, useRef, useState } from 'react';
import { message } from 'antd';
import type { Evaluation } from '../../../types/evaluation';
import {
  executeEvaluation,
  getExecutionStatus,
  createEvalSession,
  generateEvalResult,
} from '../../../services/evaluation';
import { agentChatStream, executeTools, getToolStatus, continueChatStream } from '../../../services/session';
import { executeBrowserTool } from '../../../services/toolExecutor';

const sleep = (ms: number): Promise<void> =>
  new Promise((r) => setTimeout(r, ms));

export function useEvaluationExecute(): {
  execute: (evaluationId: string, evaluation: Evaluation, onRefresh?: () => Promise<void>) => Promise<void>;
  executing: boolean;
  executionProgress: string;
  foregroundModalVisible: boolean;
  foregroundLog: string[];
  foregroundLogRef: React.RefObject<HTMLDivElement | null>;
  handleCancelForeground: () => void;
} {
  const [executing, setExecuting] = useState(false);
  const [executionProgress, setExecutionProgress] = useState('');
  const [foregroundModalVisible, setForegroundModalVisible] = useState(false);
  const [foregroundLog, setForegroundLog] = useState<string[]>([]);
  const abortRef = useRef<AbortController | null>(null);
  const executingRef = useRef(false);
  const foregroundLogRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (foregroundLogRef.current) {
      foregroundLogRef.current.scrollTop = foregroundLogRef.current.scrollHeight;
    }
  }, [foregroundLog]);

  const pollExecutionStatus = useCallback(
    async (id: string): Promise<void> => {
      while (executingRef.current) {
        const status = await getExecutionStatus(id);
        setExecutionProgress(
          `进度: ${status.currentStep}/${status.totalSteps}`,
        );
        if (status.status === 'completed' || status.status === 'error') {
          return;
        }
        await sleep(2000);
      }
    },
    [],
  );

  const runToolCycle = useCallback(
    async (
      sessionId: string,
      logLines: string[],
      toolLoopCount: { current: number },
      maxToolLoops: number,
    ): Promise<void> => {
      if (toolLoopCount.current >= maxToolLoops) {
        logLines.push('[工具] 达到最大工具调用次数');
        setForegroundLog([...logLines]);
        return;
      }
      toolLoopCount.current++;

      while (executingRef.current) {
        const execResult = await executeTools(sessionId);
        if (execResult.status === 'empty') return;

        let currentResult = execResult;
        logLines.push(`[工具] 执行: ${currentResult.toolName || currentResult.toolId || ''}`);
        if (currentResult.arguments) {
          logLines.push(`[工具] 参数: ${currentResult.arguments}`);
        }
        setForegroundLog([...logLines]);

        if (!currentResult.toolId) {
          throw new Error('工具 ID 为空');
        }

        const toolId: string = currentResult.toolId;
        let pollComplete = false;
        while (executingRef.current) {
          const toolStatus = await getToolStatus(sessionId, toolId);
          logLines.push(`[工具] 状态: ${toolStatus.status}`);
          setForegroundLog([...logLines]);

          if (toolStatus.status === 'done' || toolStatus.status === 'error') {
            if (toolStatus.result) {
              logLines.push(`[工具] 结果: ${toolStatus.result}`);
              setForegroundLog([...logLines]);
            }
            if (toolStatus.hasMore) {
              const nextExec = await executeTools(sessionId);
              if (nextExec.status === 'empty') return;
              logLines.push(
                `[工具] 继续执行: ${nextExec.toolName || nextExec.toolId || ''}`,
              );
              setForegroundLog([...logLines]);
              currentResult = nextExec;
              continue;
            }
            pollComplete = true;
            break;
          }
          if (toolStatus.toolConfig?.subToolType === 'BROWSER') {
            await executeBrowserTool(sessionId, toolId, toolStatus);
            await sleep(500);
            continue;
          }
          await sleep(1000);
        }

        if (pollComplete) {
          break;
        }
      }

      return new Promise<void>((resolve, reject) => {
        const controller = continueChatStream(sessionId, {
          onDelta: (text) => {
            const last = logLines[logLines.length - 1];
            if (last?.startsWith('[AI]')) {
              logLines[logLines.length - 1] = last + text;
            } else {
              logLines.push(`[AI] ${text}`);
            }
            setForegroundLog([...logLines]);
          },
          onReasoning: (text) => {
            const last = logLines[logLines.length - 1];
            if (last?.startsWith('[思考]')) {
              logLines[logLines.length - 1] = last + text;
            } else {
              logLines.push(`[思考] ${text}`);
            }
            setForegroundLog([...logLines]);
          },
          onDone: async (moreToolCalls) => {
            if (moreToolCalls) {
              try {
                await runToolCycle(sessionId, logLines, toolLoopCount, maxToolLoops);
                resolve();
              } catch (err) {
                reject(err);
              }
            } else {
              resolve();
            }
          },
          onError: (err) => reject(err),
        });
        abortRef.current = controller;
      });
    },
    [],
  );

  const sendForegroundMessage = useCallback(
    async (
      sessionId: string,
      content: string,
      logLines: string[],
    ): Promise<void> => {
      const toolLoopCount = { current: 0 };
      const MAX_TOOL_LOOPS = 10;

      return new Promise<void>((resolve, reject) => {
        const controller = agentChatStream(
          { sessionId, content },
          {
            onDelta: (text) => {
              const last = logLines[logLines.length - 1];
              if (last?.startsWith('[AI]')) {
                logLines[logLines.length - 1] = last + text;
              } else {
                logLines.push(`[AI] ${text}`);
              }
              setForegroundLog([...logLines]);
            },
            onReasoning: (text) => {
              const last = logLines[logLines.length - 1];
              if (last?.startsWith('[思考]')) {
                logLines[logLines.length - 1] = last + text;
              } else {
                logLines.push(`[思考] ${text}`);
              }
              setForegroundLog([...logLines]);
            },
            onDone: async (hasToolCalls) => {
              if (hasToolCalls) {
                try {
                  await runToolCycle(sessionId, logLines, toolLoopCount, MAX_TOOL_LOOPS);
                } catch (err) {
                  reject(err);
                  return;
                }
              }
              resolve();
            },
            onError: (err) => reject(err),
          },
        );
        abortRef.current = controller;
      });
    },
    [runToolCycle],
  );

  const execute = useCallback(
    async (
      evaluationId: string,
      evaluation: Evaluation,
      onRefresh?: () => Promise<void>,
    ): Promise<void> => {
      if (!evaluationId || !evaluation) return;
      setExecuting(true);
      executingRef.current = true;
      const executionType = evaluation.executionType || 'BACKGROUND';

      try {
        if (executionType === 'BACKGROUND') {
          for (let i = 0; i < evaluation.executionCount; i++) {
            if (!executingRef.current) break;
            setExecutionProgress(`执行中(第 ${i + 1}/${evaluation.executionCount} 次)...`);
            await executeEvaluation(evaluationId);
            await pollExecutionStatus(evaluationId);
          }
          message.success('后台执行完成');
        } else {
          setForegroundLog([]);
          setForegroundModalVisible(true);
          const logLines: string[] = [];

          for (let i = 0; i < evaluation.executionCount; i++) {
            if (!executingRef.current) break;
            logLines.push(`\n========== 第 ${i + 1}/${evaluation.executionCount} 次执行 ==========`);
            setForegroundLog([...logLines]);

            logLines.push('正在创建评估会话...');
            setForegroundLog([...logLines]);

            const evalSession = await createEvalSession(evaluationId);
            logLines.push(`会话已创建: ${evalSession.sessionId}`);
            logLines.push(`待处理消息数: ${evalSession.userMessages.length}`);
            setForegroundLog([...logLines]);

            for (let j = 0; j < evalSession.userMessages.length; j++) {
              if (!executingRef.current) break;
              const msg = evalSession.userMessages[j];
              logLines.push(`\n--- 消息 ${j + 1}/${evalSession.userMessages.length} ---`);
              logLines.push(`[用户] ${msg}`);
              setForegroundLog([...logLines]);

              await sendForegroundMessage(evalSession.sessionId, msg, logLines);
            }

            if (!executingRef.current) break;

            logLines.push('\n正在生成评估结果...');
            setForegroundLog([...logLines]);
            await generateEvalResult(evaluationId, evalSession.sessionId);
            logLines.push('评估结果已生成');
            setForegroundLog([...logLines]);
          }

          if (executingRef.current) {
            message.success('前台执行完成');
          }
        }
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') {
          return;
        }
        message.error('执行失败: ' + (err instanceof Error ? err.message : String(err)));
      } finally {
        setExecuting(false);
        executingRef.current = false;
        setExecutionProgress('');
        if (onRefresh) {
          await onRefresh();
        }
        if (executionType !== 'BACKGROUND') {
          await sleep(1500);
          setForegroundModalVisible(false);
        }
      }
    },
    [pollExecutionStatus, sendForegroundMessage],
  );

  const handleCancelForeground = (): void => {
    if (abortRef.current) {
      abortRef.current.abort();
    }
    executingRef.current = false;
    setExecuting(false);
    setForegroundModalVisible(false);
  };

  return {
    execute,
    executing,
    executionProgress,
    foregroundModalVisible,
    foregroundLog,
    foregroundLogRef,
    handleCancelForeground,
  };
}
