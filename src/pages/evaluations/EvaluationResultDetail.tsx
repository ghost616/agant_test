import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Button, Spin, Typography } from 'antd';
import {
  UserOutlined,
  RobotOutlined,
  ToolOutlined,
  InfoCircleOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import { getEvaluationResult } from '../../services/evaluation';
import { getSessionMessages } from '../../services/session';
import type { EvaluationResult } from '../../types/evaluation';
import type { SessionMessage } from '../../types/session';

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
  user: { background: '#1a3a5c', borderRadius: 12, padding: '10px 14px' },
  assistant: { background: '#2a2a2a', borderRadius: 12, padding: '10px 14px' },
  tool: { background: '#3a3a3a', borderRadius: 12, padding: '10px 14px' },
  system: { background: '#2d3748', borderRadius: 12, padding: '10px 14px' },
};

function EvaluationResultDetail(): JSX.Element {
  const { resultId } = useParams<{ resultId: string }>();
  const navigate = useNavigate();
  const [result, setResult] = useState<EvaluationResult | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!resultId) return;
    (async () => {
      try {
        const res = await getEvaluationResult(resultId);
        setResult(res);
        if (res.evaluationSessionId) {
          const historyMessages = await getSessionMessages(res.evaluationSessionId);
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
        }
      } catch {
        // error silently
      } finally {
        setLoading(false);
      }
    })();
  }, [resultId]);

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
            <div style={BUBBLE_STYLES[msg.role]}>
              <div style={{ color: '#d4d4d4', fontSize: 14, lineHeight: 1.8 }}>
                <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.content}</ReactMarkdown>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin tip="加载中..." />
      </div>
    );
  }

  const evaluationId = result?.evaluationId;

  return (
    <div>
      <style>{`
        .eval-detail-markdown pre {
          background: #2d2d2d;
          border-radius: 6px;
          padding: 12px 16px;
          overflow-x: auto;
        }
        .eval-detail-markdown code {
          font-family: 'Consolas', 'Courier New', monospace;
          font-size: 13px;
        }
        .eval-detail-markdown :not(pre) > code {
          background: #2d2d2d;
          padding: 2px 6px;
          border-radius: 4px;
        }
        .eval-detail-markdown table {
          border-collapse: collapse;
          width: 100%;
          margin: 12px 0;
        }
        .eval-detail-markdown th,
        .eval-detail-markdown td {
          border: 1px solid #444;
          padding: 8px 12px;
          text-align: left;
        }
        .eval-detail-markdown th {
          background: #2d2d2d;
          font-weight: 600;
        }
        .eval-detail-markdown blockquote {
          border-left: 3px solid #555;
          padding-left: 12px;
          margin: 12px 0;
          color: #aaa;
        }
        .eval-detail-markdown a {
          color: #569cd6;
        }
        .eval-detail-markdown ul,
        .eval-detail-markdown ol {
          padding-left: 24px;
        }
        .eval-detail-markdown p {
          margin: 8px 0;
        }
      `}</style>

      <div style={{ marginBottom: 16 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          onClick={() => {
            if (evaluationId) {
              navigate(`/evaluations/items/${evaluationId}/results`);
            } else {
              navigate('/evaluations');
            }
          }}
        >
          返回
        </Button>
      </div>

      <h3 style={{ marginBottom: 16 }}>评估结果详情</h3>

      <div
        style={{
          background: '#1e1e1e',
          borderRadius: 8,
          padding: 16,
          marginBottom: 24,
          maxHeight: 400,
          overflowY: 'auto',
        }}
      >
        {messages.length === 0 && (
          <Typography.Text style={{ color: '#6a6a6a' }}>暂无会话历史消息</Typography.Text>
        )}
        {messages.map((msg, idx) => renderMessage(msg, idx))}
      </div>

      <h4 style={{ marginBottom: 12 }}>评估结果</h4>
      <div
        style={{
          background: '#1e1e1e',
          borderRadius: 8,
          padding: 16,
          minHeight: 100,
        }}
        className="eval-detail-markdown"
      >
        {result?.result ? (
          <div style={{ color: '#d4d4d4', fontSize: 14, lineHeight: 1.8 }}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{result.result}</ReactMarkdown>
          </div>
        ) : (
          <Typography.Text style={{ color: '#6a6a6a' }}>暂无评估结果</Typography.Text>
        )}
      </div>
    </div>
  );
}

export default EvaluationResultDetail;
