import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Button, Input, message, Modal, Select, Spin, Switch, Table, Tabs, Typography } from 'antd';
import {
  ReloadOutlined,
  UserOutlined,
  RobotOutlined,
  ToolOutlined,
  InfoCircleOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import {
  agentChatStream,
  completeSubSession,
  continueChatStream,
  executeTools,
  getSession,
  getSessionMessages,
  getSubSessionData,
  getToolStatus,
  listChildSessions,
  rollbackSession,
  stopChat,
} from '../../services/session';
import { listModels } from '../../services/model';
import type { Session, SessionMessage } from '../../types/session';
import type { ModelConfig } from '../../types/model';

type MessageRole = 'user' | 'assistant' | 'tool' | 'system';

interface ChatMessage {
  role: MessageRole;
  content: string;
  reasoning?: string;
  toolResult?: string;
}

const ROLE_CONFIG: Record<MessageRole, { label: string; icon: JSX.Element; color: string }> = {
  user: { label: '你', icon: <UserOutlined />, color: '#569cd6' },
  assistant: { label: '助手', icon: <RobotOutlined />, color: '#4ec9b0' },
  tool: { label: '工具', icon: <ToolOutlined />, color: '#d7ba7d' },
  system: { label: '系统', icon: <InfoCircleOutlined />, color: '#9cdcfe' },
};

const BUBBLE_STYLES: Record<MessageRole, React.CSSProperties> = {
  user: {
    background: '#1a3a5c',
    borderRadius: 12,
    padding: '10px 14px',
  },
  assistant: {
    background: '#2a2a2a',
    borderRadius: 12,
    padding: '10px 14px',
  },
  tool: {
    background: '#3a3a3a',
    borderRadius: 12,
    padding: '10px 14px',
  },
  system: {
    background: '#2d3748',
    borderRadius: 12,
    padding: '10px 14px',
  },
};

function AgentChat(): JSX.Element {
  const { id } = useParams<{ id: string }>();
  const sessionId = id!;
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [toolExecuting, setToolExecuting] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [currentResponse, setCurrentResponse] = useState('');
  const [currentReasoning, setCurrentReasoning] = useState('');
  const [thinking, setThinking] = useState(false);
  const [modelId, setModelId] = useState<string | undefined>(undefined);
  const [modelList, setModelList] = useState<ModelConfig[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);
  const toolAbortRef = useRef(false);
  const hasResponseRef = useRef(false);
  const calledRef = useRef(false);
  const executeToolLoopRef = useRef<() => Promise<void>>();
  const handleSubSessionFlowRef = useRef<(toolId: string) => Promise<void>>();
  const toolCallCounts = useRef<Map<string, number>>(new Map());

  const [subSessionModalVisible, setSubSessionModalVisible] = useState(false);
  const [subSessionId, setSubSessionId] = useState<string | null>(null);
  const [subMessages, setSubMessages] = useState<ChatMessage[]>([]);
  const [subCurrentResponse, setSubCurrentResponse] = useState('');
  const [subCurrentReasoning, setSubCurrentReasoning] = useState('');
  const [subLoading, setSubLoading] = useState(false);
  const [subToolExecuting, setSubToolExecuting] = useState(false);
  const subAbortRef = useRef<AbortController | null>(null);
  const subToolAbortRef = useRef(false);
  const subContainerRef = useRef<HTMLDivElement>(null);

  const [activeTab, setActiveTab] = useState<string>('main');
  const [childSessions, setChildSessions] = useState<Session[]>([]);
  const [childSessionsLoading, setChildSessionsLoading] = useState(false);
  const [viewingChildId, setViewingChildId] = useState<string | null>(null);
  const [viewingChildMessages, setViewingChildMessages] = useState<ChatMessage[]>([]);
  const [viewingChildLoading, setViewingChildLoading] = useState(false);
  const childListLoadedRef = useRef(false);
  const childMessagesCalledRef = useRef<string | null>(null);

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [messages, currentResponse, currentReasoning]);

  useEffect(() => {
    if (subContainerRef.current) {
      subContainerRef.current.scrollTop = subContainerRef.current.scrollHeight;
    }
  }, [subMessages, subCurrentResponse, subCurrentReasoning]);

  const loadHistory = useCallback(async (): Promise<void> => {
    try {
      const [session, models, historyMessages] = await Promise.all([
        getSession(sessionId),
        listModels({ status: 'ENABLED' }),
        getSessionMessages(sessionId),
      ]);
      setModelList(models);
      setModelId(session.modelId);
      const mapped: ChatMessage[] = historyMessages.map((msg: SessionMessage) => {
        let content = msg.content;
        if (msg.role === 'tool' && msg.toolResult) {
          try {
            const tr = JSON.parse(msg.toolResult);
            content = `**工具: ${tr.toolName}**\n\n**参数:**\n\`\`\`json\n${tr.arguments}\n\`\`\`\n\n**执行结果:**\n${tr.result}`;
          } catch {
            // keep original content
          }
        }
        return {
          role: (['user', 'assistant', 'tool', 'system'].includes(msg.role)
            ? msg.role
            : 'assistant') as MessageRole,
          content,
          reasoning: msg.reasoning || undefined,
          toolResult: msg.toolResult || undefined,
        };
      });
      setMessages(mapped);
    } catch {
      message.error('加载历史消息失败');
    } finally {
      setHistoryLoading(false);
    }
  }, [sessionId]);

  const loadChildSessions = useCallback(async (): Promise<void> => {
    setChildSessionsLoading(true);
    try {
      const list = await listChildSessions(sessionId);
      setChildSessions(list);
    } catch {
      message.error('加载子会话列表失败');
    } finally {
      setChildSessionsLoading(false);
    }
  }, [sessionId]);

  const loadChildMessages = useCallback(async (childId: string): Promise<void> => {
    setViewingChildLoading(true);
    try {
      const historyMessages = await getSessionMessages(childId);
      const mapped: ChatMessage[] = historyMessages.map((msg: SessionMessage) => {
        let content = msg.content;
        if (msg.role === 'tool' && msg.toolResult) {
          try {
            const tr = JSON.parse(msg.toolResult);
            content = `**工具: ${tr.toolName}**\n\n**参数:**\n\`\`\`json\n${tr.arguments}\n\`\`\`\n\n**执行结果:**\n${tr.result}`;
          } catch {
            // keep original content
          }
        }
        return {
          role: (['user', 'assistant', 'tool', 'system'].includes(msg.role)
            ? msg.role
            : 'assistant') as MessageRole,
          content,
          reasoning: msg.reasoning || undefined,
          toolResult: msg.toolResult || undefined,
        };
      });
      setViewingChildMessages(mapped);
    } catch {
      message.error('加载子会话消息失败');
    } finally {
      setViewingChildLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!sessionId || calledRef.current) return;
    calledRef.current = true;

    loadHistory();
  }, [sessionId, loadHistory]);

  const handleAbort = useCallback(() => {
    stopChat(sessionId).catch(() => {});
    toolAbortRef.current = true;
    subToolAbortRef.current = true;
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    if (subAbortRef.current) {
      subAbortRef.current.abort();
      subAbortRef.current = null;
    }
  }, [sessionId]);

  useEffect(() => {
    return () => handleAbort();
  }, [handleAbort]);

  const pollToolStatus = useCallback(async (sid: string, toolId: string): Promise<boolean> => {
    let done = false;
    while (!done && !toolAbortRef.current) {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      if (toolAbortRef.current) return false;
      const status = await getToolStatus(sid, toolId);
      if (status.status === 'done') {
        done = true;
        setMessages((msgs) => {
          const updated = [...msgs];
          const lastIdx = updated.length - 1;
          if (lastIdx >= 0 && updated[lastIdx].role === 'tool') {
            updated[lastIdx] = {
              role: 'tool',
              content: `**工具: ${status.toolName}**\n\n**参数:**\n\`\`\`json\n${status.arguments}\n\`\`\`\n\n**执行结果:**\n${status.result || '无返回结果'}`,
            };
          }
          return updated;
        });
        return true;
      }
      if (status.needsSubSessionFlow) {
        await handleSubSessionFlowRef.current!(toolId);
        continue;
      }
      if (status.status === 'idle') {
        continue;
      }
      if (status.status === 'failed' || status.status === 'error') {
        done = true;
        setMessages((msgs) => {
          const updated = [...msgs];
          const lastIdx = updated.length - 1;
          if (lastIdx >= 0 && updated[lastIdx].role === 'tool') {
            updated[lastIdx] = {
              role: 'tool',
              content: `**工具: ${status.toolName}**\n\n**参数:**\n\`\`\`json\n${status.arguments}\n\`\`\`\n\n**执行失败:** ${status.result || '未知错误'}`,
            };
          }
          return updated;
        });
        return false;
      }
    }
    return false;
  }, []);

  const handleSubSessionFlow = useCallback(async (toolId: string): Promise<void> => {
    try {
      const data = await getSubSessionData(sessionId);
      if (!data) {
        message.error('获取子会话数据失败');
        return;
      }
      const childId = data.childSessionId;
      setSubSessionId(childId);
      setSubMessages([{ role: 'user', content: data.userMessage }]);
      setSubCurrentResponse('');
      setSubCurrentReasoning('');
      subToolAbortRef.current = false;
      setSubSessionModalVisible(true);

      const runSubChat = async (): Promise<void> => {
        const sendMessage = (content: string): Promise<boolean> =>
          new Promise((resolve) => {
            setSubLoading(true);
            setSubCurrentResponse('');
            setSubCurrentReasoning('');
            subAbortRef.current = agentChatStream(
              { sessionId: childId, content },
              {
                onDelta: (text) => setSubCurrentResponse((prev) => prev + text),
                onReasoning: (text) => setSubCurrentReasoning((prev) => prev + text),
                onDone: (hasToolCalls) => {
                  setSubCurrentResponse((prev) => {
                    setSubCurrentReasoning((reasoning) => {
                      if ((prev && prev.trim()) || (reasoning && reasoning.trim())) {
                        setSubMessages((msgs) => [
                          ...msgs,
                          { role: 'assistant', content: prev, reasoning: reasoning || undefined },
                        ]);
                      }
                      return '';
                    });
                    return '';
                  });
                  setSubLoading(false);
                  resolve(hasToolCalls);
                },
                onError: (err) => {
                  message.error(err.message || '子会话请求失败');
                  setSubLoading(false);
                  resolve(false);
                },
              },
            );
          });

        const continueChat = (): Promise<boolean> =>
          new Promise((resolve) => {
            setSubLoading(true);
            setSubCurrentResponse('');
            setSubCurrentReasoning('');
            subAbortRef.current = continueChatStream(childId, {
              onDelta: (text) => setSubCurrentResponse((prev) => prev + text),
              onReasoning: (text) => setSubCurrentReasoning((prev) => prev + text),
              onDone: (hasToolCalls) => {
                setSubCurrentResponse((prev) => {
                  setSubCurrentReasoning((reasoning) => {
                    if ((prev && prev.trim()) || (reasoning && reasoning.trim())) {
                      setSubMessages((msgs) => [
                        ...msgs,
                        { role: 'assistant', content: prev, reasoning: reasoning || undefined },
                      ]);
                    }
                    return '';
                  });
                  return '';
                });
                setSubLoading(false);
                resolve(hasToolCalls);
              },
              onError: (err) => {
                message.error(err.message || '子会话请求失败');
                setSubLoading(false);
                resolve(false);
              },
            });
          });

        const pollSubToolStatus = async (sid: string, tid: string): Promise<boolean> =>
          new Promise<boolean>((resolve) => {
            let done = false;
            const poll = async (): Promise<void> => {
              while (!done && !subToolAbortRef.current) {
                await new Promise((r) => setTimeout(r, 1000));
                if (subToolAbortRef.current) { resolve(false); return; }
                try {
                  const status = await getToolStatus(sid, tid);
                  if (status.status === 'done') {
                    done = true;
                    setSubMessages((msgs) => {
                      const updated = [...msgs];
                      const lastIdx = updated.length - 1;
                      if (lastIdx >= 0 && updated[lastIdx].role === 'tool') {
                        updated[lastIdx] = {
                          role: 'tool',
                          content: `**工具: ${status.toolName}**\n\n**参数:**\n\`\`\`json\n${status.arguments}\n\`\`\`\n\n**执行结果:**\n${status.result || '无返回结果'}`,
                        };
                      }
                      return updated;
                    });
                    resolve(true);
                    return;
                  }
                  if (status.status === 'idle') continue;
                  if (status.status === 'failed' || status.status === 'error') {
                    done = true;
                    setSubMessages((msgs) => {
                      const updated = [...msgs];
                      const lastIdx = updated.length - 1;
                      if (lastIdx >= 0 && updated[lastIdx].role === 'tool') {
                        updated[lastIdx] = {
                          role: 'tool',
                          content: `**工具: ${status.toolName}**\n\n**参数:**\n\`\`\`json\n${status.arguments}\n\`\`\`\n\n**执行失败:** ${status.result || '未知错误'}`,
                        };
                      }
                      return updated;
                    });
                    resolve(false);
                    return;
                  }
                } catch {
                  done = true;
                  resolve(false);
                  return;
                }
              }
              resolve(false);
            };
            poll();
          });

        const runTools = async (): Promise<boolean> => {
          setSubToolExecuting(true);
          subToolAbortRef.current = false;
          try {
            let hasMore = true;
            while (hasMore && !subToolAbortRef.current) {
              const execResult = await executeTools(childId);
              if (subToolAbortRef.current) break;
              if (execResult.status === 'empty') {
                hasMore = false;
                continue;
              }
              hasMore = execResult.hasMore;
              if (!execResult.toolId) {
                hasMore = false;
                continue;
              }
              const key = `${execResult.toolName}:${execResult.arguments}`;
              const count = (toolCallCounts.current.get(key) || 0) + 1;
              toolCallCounts.current.set(key, count);
              if (count >= 5) {
                message.warning(`子会话工具 ${execResult.toolName} 同一参数调用已达 ${count} 次，已终止`);
                hasMore = false;
                continue;
              }
              setSubMessages((prev) => [
                ...prev,
                {
                  role: 'tool',
                  content: `**正在执行工具: ${execResult.toolName}**\n\n**参数:**\n\`\`\`json\n${execResult.arguments}\n\`\`\``,
                },
              ]);
              const succeeded = await pollSubToolStatus(childId, execResult.toolId);
              if (!succeeded) hasMore = false;
            }
            return !subToolAbortRef.current;
          } catch {
            message.error('子会话工具执行失败');
            return false;
          } finally {
            setSubToolExecuting(false);
          }
        };

        let hasToolCalls = await sendMessage(data.userMessage);
        while (hasToolCalls && !subToolAbortRef.current) {
          const ok = await runTools();
          if (!ok) break;
          hasToolCalls = await continueChat();
        }
      };

      await runSubChat();
      await completeSubSession(sessionId);
      setSubSessionModalVisible(false);
      const succeeded = await pollToolStatus(sessionId, toolId);
      if (!succeeded) {
        setToolExecuting(false);
        setLoading(false);
        abortRef.current = null;
      }
    } catch {
      message.error('子会话流程执行失败');
      setToolExecuting(false);
      setLoading(false);
      abortRef.current = null;
    }
  }, [sessionId, pollToolStatus]);

  const executeToolLoop = useCallback(async () => {
    setToolExecuting(true);
    toolAbortRef.current = false;
    try {
      let hasMore = true;
      let hadTools = false;
      while (hasMore && !toolAbortRef.current) {
        const execResult = await executeTools(sessionId);
        if (toolAbortRef.current) break;
        if (execResult.status === 'empty') {
          hasMore = false;
          continue;
        }
        hasMore = execResult.hasMore;
        if (!execResult.toolId) {
          hasMore = false;
          continue;
        }
        hadTools = true;
        const key = `${execResult.toolName}:${execResult.arguments}`;
        const count = (toolCallCounts.current.get(key) || 0) + 1;
        toolCallCounts.current.set(key, count);
        if (count >= 5) {
          message.warning(`工具 ${execResult.toolName} 同一参数调用已达 ${count} 次，已终止`);
          hasMore = false;
          continue;
        }
        setMessages((prev) => [
          ...prev,
          {
            role: 'tool',
            content: `**正在执行工具: ${execResult.toolName}**\n\n**参数:**\n\`\`\`json\n${execResult.arguments}\n\`\`\``,
          },
        ]);
        const succeeded = await pollToolStatus(sessionId, execResult.toolId);
        if (!succeeded) hasMore = false;
      }
      if (toolAbortRef.current) {
        setToolExecuting(false);
        setLoading(false);
        abortRef.current = null;
        return;
      }
      if (!hadTools) {
        setToolExecuting(false);
        setLoading(false);
        abortRef.current = null;
        return;
      }
      setToolExecuting(false);
      setCurrentResponse('');
      setCurrentReasoning('');
      hasResponseRef.current = false;
      abortRef.current = continueChatStream(
        sessionId,
        {
          onDelta: (text) => {
            hasResponseRef.current = true;
            setCurrentResponse((prev) => prev + text);
          },
          onReasoning: (text) => {
            hasResponseRef.current = true;
            setCurrentReasoning((prev) => prev + text);
          },
          onDone: (hasMoreTools) => {
            setCurrentResponse((prev) => {
              setCurrentReasoning((reasoning) => {
                if ((prev && prev.trim()) || (reasoning && reasoning.trim())) {
                  setMessages((msgs) => [
                    ...msgs,
                    {
                      role: 'assistant',
                      content: prev,
                      reasoning: reasoning || undefined,
                    },
                  ]);
                }
                return '';
              });
              return '';
            });
            if (hasMoreTools) {
              executeToolLoopRef.current?.();
            } else {
              toolCallCounts.current.clear();
              setLoading(false);
              abortRef.current = null;
            }
          },
          onError: (err) => {
            message.error(err.message || '请求失败');
            setLoading(false);
            abortRef.current = null;
          },
        },
      );
    } catch {
      message.error('工具执行失败');
      setToolExecuting(false);
      setLoading(false);
      abortRef.current = null;
    }
  }, [sessionId, pollToolStatus]);

  handleSubSessionFlowRef.current = handleSubSessionFlow;

  executeToolLoopRef.current = executeToolLoop;

  const renderSubSessionModal = (): JSX.Element => (
    <Modal
      title="子会话对话"
      open={subSessionModalVisible}
      onCancel={() => {
        subToolAbortRef.current = true;
        if (subAbortRef.current) {
          subAbortRef.current.abort();
          subAbortRef.current = null;
        }
        setSubSessionModalVisible(false);
      }}
      width={800}
      footer={null}
      destroyOnClose
    >
      <div
        ref={subContainerRef}
        style={{
          background: '#1e1e1e',
          borderRadius: 8,
          padding: 16,
          overflowY: 'auto',
          maxHeight: '60vh',
          minHeight: 200,
        }}
      >
        {subMessages.map((msg, idx) => renderMessage(msg, idx))}

        {subToolExecuting && (
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-start',
              marginBottom: 16,
            }}
          >
            <div style={{ maxWidth: '75%' }}>
              {renderRoleHeader('assistant')}
              <div style={{ marginTop: 8 }}>
                <Spin size="small" />
                <Typography.Text style={{ color: '#aaa', fontSize: 12, marginLeft: 8 }}>
                  正在执行工具调用...
                </Typography.Text>
              </div>
            </div>
          </div>
        )}

        {subLoading && !subToolExecuting && (
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-start',
              marginBottom: 16,
            }}
          >
            <div style={{ maxWidth: '75%' }}>
              {renderRoleHeader('assistant')}
              {subCurrentReasoning && renderReasoning(subCurrentReasoning)}
              {subCurrentResponse ? (
                <div style={BUBBLE_STYLES.assistant} className="agent-chat-markdown">
                  <div style={{ color: '#d4d4d4', fontSize: 14, lineHeight: 1.8 }}>
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>
                      {subCurrentResponse}
                    </ReactMarkdown>
                  </div>
                </div>
              ) : (
                !subCurrentReasoning && (
                  <div style={{ marginTop: 8 }}>
                    <Spin size="small" />
                  </div>
                )
              )}
            </div>
          </div>
        )}
      </div>
    </Modal>
  );

  const handleSend = useCallback(() => {
    if (!inputValue.trim() || loading) return;

    const userMsg: ChatMessage = { role: 'user', content: inputValue };
    setMessages((prev) => [...prev, userMsg]);
    setInputValue('');
    setLoading(true);
    setToolExecuting(false);
    setCurrentResponse('');
    setCurrentReasoning('');
    hasResponseRef.current = false;
    toolAbortRef.current = false;

    abortRef.current = agentChatStream(
      { sessionId, content: inputValue, modelId, thinking },
      {
        onDelta: (text: string) => {
          hasResponseRef.current = true;
          setCurrentResponse((prev) => prev + text);
        },
        onReasoning: (text: string) => {
          hasResponseRef.current = true;
          setCurrentReasoning((prev) => prev + text);
        },
        onDone: (hasToolCalls: boolean) => {
          setCurrentResponse((prev) => {
            setCurrentReasoning((reasoning) => {
              if ((prev && prev.trim()) || (reasoning && reasoning.trim())) {
                setMessages((msgs) => [
                  ...msgs,
                  {
                    role: 'assistant',
                    content: prev,
                    reasoning: reasoning || undefined,
                  },
                ]);
              }
              return '';
            });
            return '';
          });
          if (!hasResponseRef.current && !hasToolCalls) {
            message.warning('未收到回复内容');
          }
          if (hasToolCalls) {
            executeToolLoopRef.current?.();
          } else {
            setLoading(false);
            abortRef.current = null;
          }
        },
        onError: (err: Error) => {
          message.error(err.message || '请求失败');
          setLoading(false);
          abortRef.current = null;
        },
      },
    );
  }, [inputValue, loading, sessionId, modelId, thinking]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
    },
    [handleSend],
  );

  const renderRoleHeader = (role: MessageRole): JSX.Element => {
    const config = ROLE_CONFIG[role];
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginBottom: 4 }}>
        <span style={{ color: config.color, fontSize: 14 }}>{config.icon}</span>
        <Typography.Text strong style={{ color: config.color, fontSize: 12 }}>
          {config.label}
        </Typography.Text>
      </div>
    );
  };

  const renderReasoning = (reasoning: string): JSX.Element => (
    <div
      style={{
        background: '#252525',
        borderLeft: '3px solid #ffd700',
        borderRadius: 4,
        padding: '8px 12px',
        marginBottom: 8,
      }}
      className="agent-chat-markdown"
    >
      <Typography.Text
        style={{ color: '#ffd700', fontSize: 12, marginBottom: 4, display: 'block' }}
      >
        思考过程
      </Typography.Text>
      <div style={{ color: '#aaa', fontSize: 13, lineHeight: 1.7 }}>
        <ReactMarkdown remarkPlugins={[remarkGfm]}>{reasoning}</ReactMarkdown>
      </div>
    </div>
  );

  const renderMessage = (msg: ChatMessage, idx: number): JSX.Element => {
    const isUser = msg.role === 'user';
    return (
      <div
        key={idx}
        style={{
          display: 'flex',
          justifyContent: isUser ? 'flex-end' : 'flex-start',
          marginBottom: 16,
        }}
      >
        <div style={{ maxWidth: '75%' }}>
          {renderRoleHeader(msg.role)}
          {msg.reasoning && renderReasoning(msg.reasoning)}
          {msg.content.trim() && (
            <div style={BUBBLE_STYLES[msg.role]} className="agent-chat-markdown">
              <div style={{ color: '#d4d4d4', fontSize: 14, lineHeight: 1.8 }}>
                <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.content}</ReactMarkdown>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  const renderMainChat = (): JSX.Element => (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <style>{`
        .agent-chat-markdown pre {
          background: #2d2d2d;
          border-radius: 6px;
          padding: 12px 16px;
          overflow-x: auto;
        }
        .agent-chat-markdown code {
          font-family: 'Consolas', 'Courier New', monospace;
          font-size: 13px;
        }
        .agent-chat-markdown :not(pre) > code {
          background: #2d2d2d;
          padding: 2px 6px;
          border-radius: 4px;
        }
        .agent-chat-markdown table {
          border-collapse: collapse;
          width: 100%;
          margin: 12px 0;
        }
        .agent-chat-markdown th,
        .agent-chat-markdown td {
          border: 1px solid #444;
          padding: 8px 12px;
          text-align: left;
        }
        .agent-chat-markdown th {
          background: #2d2d2d;
          font-weight: 600;
        }
        .agent-chat-markdown blockquote {
          border-left: 3px solid #555;
          padding-left: 12px;
          margin: 12px 0;
          color: #aaa;
        }
        .agent-chat-markdown a {
          color: #569cd6;
        }
        .agent-chat-markdown ul,
        .agent-chat-markdown ol {
          padding-left: 24px;
        }
        .agent-chat-markdown p {
          margin: 8px 0;
        }
      `}</style>

      <div
        ref={containerRef}
        style={{
          flex: 1,
          background: '#1e1e1e',
          borderRadius: 8,
          padding: 16,
          overflowY: 'auto',
          marginBottom: 16,
          minHeight: 200,
        }}
      >
        {historyLoading && (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <Spin tip="加载历史消息..." />
          </div>
        )}

        {!historyLoading && messages.length === 0 && !loading && !toolExecuting && (
          <Typography.Text style={{ color: '#6a6a6a', fontSize: 14 }}>
            发送消息开始对话
          </Typography.Text>
        )}

        {!historyLoading && messages.map((msg, idx) => renderMessage(msg, idx))}

        {toolExecuting && (
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-start',
              marginBottom: 16,
            }}
          >
            <div style={{ maxWidth: '75%' }}>
              {renderRoleHeader('assistant')}
              <div style={{ marginTop: 8 }}>
                <Spin size="small" />
                <Typography.Text
                  style={{ color: '#aaa', fontSize: 12, marginLeft: 8 }}
                >
                  正在执行工具调用...
                </Typography.Text>
              </div>
            </div>
          </div>
        )}

        {loading && !toolExecuting && (
          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-start',
              marginBottom: 16,
            }}
          >
            <div style={{ maxWidth: '75%' }}>
              {renderRoleHeader('assistant')}
              {currentReasoning && renderReasoning(currentReasoning)}
              {currentResponse ? (
                <div style={BUBBLE_STYLES.assistant} className="agent-chat-markdown">
                  <div style={{ color: '#d4d4d4', fontSize: 14, lineHeight: 1.8 }}>
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>
                      {currentResponse}
                    </ReactMarkdown>
                  </div>
                </div>
              ) : (
                !currentReasoning && (
                  <div style={{ marginTop: 8 }}>
                    <Spin size="small" />
                  </div>
                )
              )}
            </div>
          </div>
        )}
      </div>

      <div style={{ marginBottom: 12, display: 'flex', alignItems: 'center', gap: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Typography.Text type="secondary" style={{ fontSize: 13, whiteSpace: 'nowrap' }}>
            选择模型
          </Typography.Text>
          <Select
            placeholder="选择模型"
            allowClear
            style={{ width: 200 }}
            value={modelId}
            onChange={setModelId}
            options={modelList.map((m) => ({
              value: String(m.id),
              label: m.name,
            }))}
          />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            思考模式
          </Typography.Text>
          <Switch
            checked={thinking}
            onChange={setThinking}
            size="small"
          />
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <Input.TextArea
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          rows={3}
          autoSize={{ minRows: 2, maxRows: 6 }}
        />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, minWidth: 80 }}>
          <Button
            type="primary"
            onClick={handleSend}
            disabled={loading || !inputValue.trim()}
            loading={loading}
          >
            发送
          </Button>
          {loading && (
            <Button onClick={handleAbort} danger>
              停止
            </Button>
          )}
          <Button
            disabled={loading || toolExecuting}
            onClick={async () => {
              try {
                await rollbackSession(sessionId);
                await loadHistory();
              } catch {
                message.error('回滚失败');
              }
            }}
          >
            回滚
          </Button>
        </div>
      </div>
    </div>
  );

  const renderChildSessionView = (): JSX.Element => {
    const childSession = childSessions.find((s) => s.id === viewingChildId);
    return (
      <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        <Button
          icon={<ArrowLeftOutlined />}
          style={{ marginBottom: 12, alignSelf: 'flex-start' }}
          onClick={() => setViewingChildId(null)}
        >
          返回子会话列表
        </Button>
        {childSession && (
          <Typography.Title level={5} style={{ color: '#e0e0e0', marginBottom: 12 }}>
            {childSession.title || '未命名会话'}
          </Typography.Title>
        )}
        <div
          style={{
            flex: 1,
            background: '#1e1e1e',
            borderRadius: 8,
            padding: 16,
            overflowY: 'auto',
            minHeight: 200,
          }}
        >
          {viewingChildLoading && (
            <div style={{ textAlign: 'center', padding: 40 }}>
              <Spin tip="加载消息..." />
            </div>
          )}
          {!viewingChildLoading && viewingChildMessages.length === 0 && (
            <Typography.Text style={{ color: '#6a6a6a', fontSize: 14 }}>
              暂无消息
            </Typography.Text>
          )}
          {!viewingChildLoading && viewingChildMessages.map((msg, idx) => renderMessage(msg, idx))}
        </div>
      </div>
    );
  };

  const childSessionColumns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      render: (text: string) => text || '未命名会话',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (text: string) => text || '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: Session) => (
        <Button
          type="link"
          onClick={() => {
            setViewingChildId(record.id);
            childMessagesCalledRef.current = null;
          }}
        >
          查看会话
        </Button>
      ),
    },
  ];

  const renderChildSessionList = (): JSX.Element => (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
        <Button
          icon={<ReloadOutlined />}
          loading={childSessionsLoading}
          onClick={() => loadChildSessions()}
        >
          刷新
        </Button>
      </div>
      {childSessionsLoading && (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin tip="加载子会话列表..." />
        </div>
      )}
      {!childSessionsLoading && childSessions.length === 0 && (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Typography.Text style={{ color: '#6a6a6a', fontSize: 14 }}>
            暂无子会话
          </Typography.Text>
        </div>
      )}
      {!childSessionsLoading && childSessions.length > 0 && (
        <Table
          dataSource={childSessions}
          columns={childSessionColumns}
          rowKey="id"
          pagination={false}
          style={{ background: 'transparent' }}
        />
      )}
    </div>
  );

  const renderChildTab = (): JSX.Element => {
    if (viewingChildId) {
      return renderChildSessionView();
    }
    return renderChildSessionList();
  };

  if (!id) {
    return (
      <div style={{ textAlign: 'center', paddingTop: 100 }}>
        <Typography.Text type="secondary">无效的会话</Typography.Text>
      </div>
    );
  }

  const handleTabChange = (key: string): void => {
    setActiveTab(key);
    if (key === 'children' && !childListLoadedRef.current) {
      childListLoadedRef.current = true;
      loadChildSessions();
    }
  };

  useEffect(() => {
    if (viewingChildId && childMessagesCalledRef.current !== viewingChildId) {
      childMessagesCalledRef.current = viewingChildId;
      loadChildMessages(viewingChildId);
    }
  }, [viewingChildId, loadChildMessages]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 180px)' }}>
      {renderSubSessionModal()}
      <style>{`
        .agent-chat-tabs {
          display: flex;
          flex-direction: column;
          height: 100%;
        }
        .agent-chat-tabs .ant-tabs-nav {
          margin-bottom: 0;
        }
        .agent-chat-tabs .ant-tabs-content-holder {
          flex: 1;
          overflow: hidden;
        }
        .agent-chat-tabs .ant-tabs-content {
          height: 100%;
        }
        .agent-chat-tabs .ant-tabs-tabpane {
          height: 100%;
        }
      `}</style>
      <Tabs
        className="agent-chat-tabs"
        activeKey={activeTab}
        onChange={handleTabChange}
        style={{ display: 'flex', flexDirection: 'column', height: '100%' }}
        items={[
          { key: 'main', label: '主会话', children: renderMainChat() },
          { key: 'children', label: '子会话列表', children: renderChildTab() },
        ]}
      />
    </div>
  );
}

export default AgentChat;
