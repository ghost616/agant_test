import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Button, Input, message, Modal, Select, Spin, Switch, Tabs, Typography } from 'antd';
import type { TabsProps } from 'antd';
import {
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
  fetchConversationId,
  getSession,
  getSessionContextBasic,
  getSessionMessages,
  getSubSessionData,
  getToolStatus,
  listChildSessions,
  rollbackSession,
  stopChat,
  updateSessionThinking,
} from '../../services/session';
import { executeBrowserTool } from '../../services/toolExecutor';
import { listModels } from '../../services/model';
import {
  registerSessionPage,
  SEND_USER_MESSAGE_MARKER,
  unregisterSessionPage,
} from '../../services/messageDispatcher';
import type { SendUserMessagePayload, SessionPageHandler } from '../../services/messageDispatcher';
import type { Session, SessionMessage, ToolInfo, WebSearchCall } from '../../types/session';
import type { ModelConfig } from '../../types/model';

type MessageRole = 'user' | 'assistant' | 'tool' | 'system';

interface ChatMessage {
  role: MessageRole;
  content: string;
  reasoning?: string;
  toolResult?: string;
  toolInfo?: ToolInfo;
  webSearchCall?: WebSearchCall[];
}

/** 子会话流式回复展示状态（WS 消息分发触发，展示到子会话标签视图）。 */
interface ChildStreamState {
  messages: ChatMessage[];
  currentResponse: string;
  currentReasoning: string;
  loading: boolean;
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

/**
 * 将后端会话消息映射为前端聊天消息。
 * @param historyMessages 后端会话消息列表
 * @returns 前端聊天消息列表
 */
function mapSessionMessages(historyMessages: SessionMessage[]): ChatMessage[] {
  return historyMessages.map((msg: SessionMessage) => {
    let content = msg.content;
    if (msg.role === 'tool' && msg.toolResult) {
      try {
        const tr = JSON.parse(msg.toolResult);
        const toolName = msg.toolInfo?.toolName || tr.toolName;
        content = `**工具: ${toolName}**\n\n**参数:**\n\`\`\`json\n${tr.arguments}\n\`\`\`\n\n**执行结果:**\n${tr.result}`;
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
      toolInfo: msg.toolInfo || undefined,
      webSearchCall: msg.webSearchCall || undefined,
    };
  });
}

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

const renderWebSearchCall = (calls: WebSearchCall[]): JSX.Element => (
  <div
    style={{
      background: '#1e3a4f',
      borderLeft: '3px solid #569cd6',
      borderRadius: 4,
      padding: '8px 12px',
      marginBottom: 8,
    }}
  >
    <Typography.Text style={{ color: '#9cdcfe', fontSize: 12, marginBottom: 4, display: 'block' }}>
      搜索结果
    </Typography.Text>
    {calls.map((call, ci) => (
      <div key={ci}>
        {call.results.map((r, i) => (
          <div key={i} style={{ marginBottom: 6 }}>
            <a
              href={r.url}
              target="_blank"
              rel="noopener noreferrer"
              style={{ color: '#569cd6', fontSize: 13 }}
            >
              {r.title}
            </a>
            <div style={{ color: '#aaa', fontSize: 12, lineHeight: 1.6, marginTop: 2 }}>
              {r.snippet}
            </div>
          </div>
        ))}
      </div>
    ))}
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
        {msg.webSearchCall && msg.webSearchCall.length > 0 && renderWebSearchCall(msg.webSearchCall)}
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

/**
 * 子会话只读展示视图：加载并展示指定子会话的历史消息。
 * 收到 SEND_USER_MESSAGE 消息时可通过 stream 属性实时展示流式回复。
 * 不含输入框、模型选择、思考模式及发送/回滚/停止等交互控件。
 */
function ChildSessionView({
  childId,
  stream,
}: {
  childId: string;
  stream?: ChildStreamState;
}): JSX.Element {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    getSessionMessages(childId)
      .then((historyMessages) => {
        if (!cancelled) setMessages(mapSessionMessages(historyMessages));
      })
      .catch(() => {
        if (!cancelled) message.error('加载子会话消息失败');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [childId]);

  // 合并历史消息与实时流式消息：历史最后一条与流式首条用户消息相同时去重，避免重复展示
  const mergedMessages = useMemo(() => {
    if (!stream || stream.messages.length === 0) {
      return messages;
    }
    const history = [...messages];
    const last = history[history.length - 1];
    const firstStream = stream.messages[0];
    if (
      last &&
      last.role === 'user' &&
      firstStream.role === 'user' &&
      last.content === firstStream.content
    ) {
      history.pop();
    }
    return [...history, ...stream.messages];
  }, [messages, stream]);

  const showStreaming =
    stream !== undefined &&
    (stream.loading || stream.currentResponse !== '' || stream.currentReasoning !== '');
  const showHistorySpinner = loading && !showStreaming;

  return (
    <div
      style={{
        flex: 1,
        background: '#1e1e1e',
        borderRadius: 8,
        padding: 16,
        overflowY: 'auto',
        minHeight: 200,
        height: '100%',
      }}
    >
      {showHistorySpinner && (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin tip="加载消息..." />
        </div>
      )}
      {!showHistorySpinner && mergedMessages.length === 0 && !showStreaming && (
        <Typography.Text style={{ color: '#6a6a6a', fontSize: 14 }}>
          暂无消息
        </Typography.Text>
      )}
      {!showHistorySpinner && mergedMessages.map((msg, idx) => renderMessage(msg, idx))}
      {showStreaming && (
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-start',
            marginBottom: 16,
          }}
        >
          <div style={{ maxWidth: '75%' }}>
            {renderRoleHeader('assistant')}
            {stream!.currentReasoning && renderReasoning(stream!.currentReasoning)}
            {stream!.currentResponse ? (
              <div style={BUBBLE_STYLES.assistant} className="agent-chat-markdown">
                <div style={{ color: '#d4d4d4', fontSize: 14, lineHeight: 1.8 }}>
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {stream!.currentResponse}
                  </ReactMarkdown>
                </div>
              </div>
            ) : (
              !stream!.currentReasoning && (
                <div style={{ marginTop: 8 }}>
                  <Spin size="small" />
                </div>
              )
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function AgentChat(): JSX.Element {
  const { id } = useParams<{ id: string }>();
  const sessionId = id!;
  const [searchParams] = useSearchParams();
  const isBenchmark = searchParams.get('benchmark') === '1';
  const returnUrlRaw = searchParams.get('returnUrl');
  const returnUrl = returnUrlRaw ? decodeURIComponent(returnUrlRaw) : '/evaluations';
  const navigate = useNavigate();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [toolExecuting, setToolExecuting] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [currentResponse, setCurrentResponse] = useState('');
  const [currentReasoning, setCurrentReasoning] = useState('');
  const [currentWebSearchCall, setCurrentWebSearchCall] = useState<WebSearchCall[]>([]);
  const [thinking, setThinking] = useState(false);
  const [modelId, setModelId] = useState<string | undefined>(undefined);
  const [modelList, setModelList] = useState<ModelConfig[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);
  const toolAbortRef = useRef(false);
  const hasResponseRef = useRef(false);
  const webSearchCallRef = useRef<WebSearchCall[]>([]);
  const calledRef = useRef(false);
  const responseIdRef = useRef<string | null>(null);
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
  // 子会话流式回复展示状态（按子会话 ID 索引，由 WS 消息分发触发）
  const [childStreams, setChildStreams] = useState<Record<string, ChildStreamState>>({});
  const activeTabRef = useRef<string>('main');
  const childStreamsRef = useRef<Record<string, ChildStreamState>>({});
  const streamChildReplyRef = useRef<(message: SendUserMessagePayload) => void>(() => {});

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
      const [session, historyMessages] = await Promise.all([
        getSession(sessionId),
        getSessionMessages(sessionId),
      ]);
      const models = await listModels({ status: 'ENABLED', modelType: 'LLM' });
      setModelList(models);
      setModelId(session.modelId);
      if (session.thinking !== undefined) setThinking(session.thinking);
      const mapped: ChatMessage[] = mapSessionMessages(historyMessages);
      setMessages(mapped);
    } catch {
      message.error('加载历史消息失败');
    } finally {
      setHistoryLoading(false);
    }
  }, [sessionId]);

  const loadChildSessions = useCallback(async (): Promise<void> => {
    try {
      const list = await listChildSessions(sessionId);
      setChildSessions(list);
    } catch {
      message.error('加载子会话列表失败');
    }
  }, [sessionId]);

  useEffect(() => {
    if (!sessionId || calledRef.current) return;
    calledRef.current = true;

    getSessionContextBasic(sessionId)
      .then((ctx) => {
        if (ctx.lastResponseId) {
          responseIdRef.current = ctx.lastResponseId;
        }
      })
      .catch(() => {});

    loadHistory();
    loadChildSessions();
  }, [sessionId, loadHistory, loadChildSessions]);

  // 同步激活标签到 ref（供 WS 消息分发读取最新值）
  useEffect(() => {
    activeTabRef.current = activeTab;
  }, [activeTab]);

  // 同步子会话流式状态到 ref（供 WS 回调判断是否正在流式）
  useEffect(() => {
    childStreamsRef.current = childStreams;
  }, [childStreams]);

  /**
   * 更新指定子会话的流式展示状态。
   * @param childId 子会话 ID
   * @param updater 状态更新函数
   */
  const updateChildStream = useCallback(
    (childId: string, updater: (state: ChildStreamState) => ChildStreamState): void => {
      setChildStreams((prev) => {
        const current = prev[childId];
        if (!current) return prev;
        return { ...prev, [childId]: updater(current) };
      });
    },
    [],
  );

  /**
   * 子会话消息分发：收到 SEND_USER_MESSAGE 且对应子会话处于激活视图时，
   * 以特殊标记 [send_user_message] 调用对话接口，流式展示该子会话的 AI 回复。
   * @param payload SEND_USER_MESSAGE 消息负载
   */
  const streamChildReply = useCallback(
    (payload: SendUserMessagePayload): void => {
      const childId = payload.sessionId;
      if (!childId) return;
      // 同一子会话已有流式请求进行中时忽略新消息（一次流式覆盖全部已持久化消息）
      if (childStreamsRef.current[childId]?.loading) return;
      const userMsg: ChatMessage = { role: 'user', content: payload.content || '' };
      setChildStreams((prev) => {
        const existing = prev[childId];
        const baseMessages = existing ? existing.messages : [];
        return {
          ...prev,
          [childId]: {
            messages: [...baseMessages, userMsg],
            currentResponse: '',
            currentReasoning: '',
            loading: true,
          },
        };
      });
      agentChatStream(
        { sessionId: childId, content: SEND_USER_MESSAGE_MARKER },
        {
          onDelta: (text) =>
            updateChildStream(childId, (s) => ({
              ...s,
              currentResponse: s.currentResponse + text,
            })),
          onReasoning: (text) =>
            updateChildStream(childId, (s) => ({
              ...s,
              currentReasoning: s.currentReasoning + text,
            })),
          onDone: () =>
            updateChildStream(childId, (s) => {
              const hasContent = Boolean(s.currentResponse.trim() || s.currentReasoning.trim());
              return {
                messages: hasContent
                  ? [
                      ...s.messages,
                      {
                        role: 'assistant',
                        content: s.currentResponse,
                        reasoning: s.currentReasoning || undefined,
                      },
                    ]
                  : s.messages,
                currentResponse: '',
                currentReasoning: '',
                loading: false,
              };
            }),
          onError: (err) => {
            message.error(err.message || '子会话回复请求失败');
            updateChildStream(childId, (s) => ({ ...s, loading: false }));
          },
        },
      );
    },
    [updateChildStream],
  );

  streamChildReplyRef.current = streamChildReply;

  // 进入会话页面：注册消息分发页面处理器
  useEffect(() => {
    if (!sessionId) return;
    const handler: SessionPageHandler = {
      mainSessionId: sessionId,
      isChildActive: (childId) => activeTabRef.current === childId,
      streamChildReply: (msg) => streamChildReplyRef.current(msg),
      refreshChildSessions: () => {
        loadChildSessions();
      },
    };
    registerSessionPage(handler);
    return () => {
      unregisterSessionPage(sessionId);
    };
  }, [sessionId, loadChildSessions]);

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
      if (status.toolConfig?.subToolType === 'BROWSER') {
        await executeBrowserTool(sid, toolId, status);
        await new Promise((resolve) => setTimeout(resolve, 500));
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
              { sessionId: childId, content, thinking: data.thinking },
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
                  if (status.toolConfig?.subToolType === 'BROWSER') {
                    await executeBrowserTool(sid, tid, status);
                    await new Promise((r) => setTimeout(r, 500));
                    continue;
                  }
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
      setCurrentWebSearchCall([]);
      hasResponseRef.current = false;
      webSearchCallRef.current = [];
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
          onWebSearchCall: (calls: WebSearchCall[]) => {
            webSearchCallRef.current = calls;
            setCurrentWebSearchCall(calls);
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
                      webSearchCall: webSearchCallRef.current.length ? webSearchCallRef.current : undefined,
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

  const handleSend = useCallback(async () => {
    if (!inputValue.trim() || loading) return;

    let conversationId: string | undefined;
    try {
      conversationId = await fetchConversationId();
    } catch {
      message.error('获取会话标识失败，请重试');
      return;
    }

    const currentModel = modelList.find((m) => String(m.id) === String(modelId));
    const isResponsesStateful = currentModel?.requestType === 'responses';
    const previousResponseId = isResponsesStateful ? responseIdRef.current || undefined : undefined;

    const userMsg: ChatMessage = { role: 'user', content: inputValue };
    setMessages((prev) => [...prev, userMsg]);
    setInputValue('');
    setLoading(true);
    setToolExecuting(false);
    setCurrentResponse('');
    setCurrentReasoning('');
    setCurrentWebSearchCall([]);
    hasResponseRef.current = false;
    webSearchCallRef.current = [];
    toolAbortRef.current = false;

    abortRef.current = agentChatStream(
      { sessionId, content: inputValue, modelId, thinking, previousResponseId, conversationId },
      {
        onDelta: (text: string) => {
          hasResponseRef.current = true;
          setCurrentResponse((prev) => prev + text);
        },
        onReasoning: (text: string) => {
          hasResponseRef.current = true;
          setCurrentReasoning((prev) => prev + text);
        },
        onResponseId: (id: string) => {
          responseIdRef.current = id;
        },
        onWebSearchCall: (calls: WebSearchCall[]) => {
          webSearchCallRef.current = calls;
          setCurrentWebSearchCall(calls);
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
                    webSearchCall: webSearchCallRef.current.length ? webSearchCallRef.current : undefined,
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
  }, [inputValue, loading, sessionId, modelId, thinking, modelList]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleSend();
      }
    },
    [handleSend],
  );

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
              {currentWebSearchCall.length > 0 && renderWebSearchCall(currentWebSearchCall)}
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
            {isBenchmark ? '模型' : '选择模型'}
          </Typography.Text>
          <Select
            placeholder="选择模型"
            allowClear
            disabled={isBenchmark}
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
            onChange={(checked) => {
              setThinking(checked);
              updateSessionThinking(sessionId, checked).catch(() => {});
            }}
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

  const tabItems: TabsProps['items'] = [
    { key: 'main', label: '主会话', children: renderMainChat() },
    ...childSessions.map((child) => ({
      key: child.id,
      label: child.title || child.id,
      children: <ChildSessionView childId={child.id} stream={childStreams[child.id]} />,
    })),
  ];

  if (!id) {
    return (
      <div style={{ textAlign: 'center', paddingTop: 100 }}>
        <Typography.Text type="secondary">无效的会话</Typography.Text>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 180px)' }}>
      {renderSubSessionModal()}
      {isBenchmark && (
        <div style={{ padding: '12px 0 0 12px' }}>
          <Button
            icon={<ArrowLeftOutlined />}
            style={{ alignSelf: 'flex-start', width: 'fit-content' }}
            onClick={() => navigate(returnUrl)}
          >
            返回评估
          </Button>
        </div>
      )}
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
        onChange={setActiveTab}
        style={{ display: 'flex', flexDirection: 'column', height: '100%' }}
        items={tabItems}
      />
    </div>
  );
}

export default AgentChat;
