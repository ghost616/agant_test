import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, message, Modal, Table, Tag, Tooltip, Typography } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { SessionMessage, ToolCallData } from '../../types/session';
import { getConversationMessages } from '../../services/session';

const ROLE_LABELS: Record<string, { text: string; color: string }> = {
  user: { text: '用户', color: 'blue' },
  assistant: { text: '助手', color: 'green' },
  tool: { text: '工具', color: 'purple' },
  system: { text: '系统', color: 'default' },
};

function shortenSessionId(id: string): string {
  if (!id || id.length <= 12) {
    return id || '-';
  }
  return `${id.slice(0, 8)}…${id.slice(-4)}`;
}

function ConversationDetail(): JSX.Element {
  const { conversationId } = useParams<{ conversationId?: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const sessionId = (location.state as { sessionId?: string } | null)?.sessionId;
  const [messages, setMessages] = useState<SessionMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailTitle, setDetailTitle] = useState('');
  const [detailContent, setDetailContent] = useState('');

  const fetchMessages = useCallback(async (cid: string): Promise<void> => {
    setLoading(true);
    try {
      const result = await getConversationMessages(cid);
      setMessages(result);
    } catch {
      message.error('获取对话消息失败');
      setMessages([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (conversationId) {
      fetchMessages(conversationId);
    }
  }, [conversationId, fetchMessages]);

  const openToolCallsModal = (toolCalls: ToolCallData[]): void => {
    setDetailTitle('工具调用');
    setDetailContent(JSON.stringify(toolCalls, null, 2));
    setDetailVisible(true);
  };

  const openToolResultModal = (toolResult?: string): void => {
    setDetailTitle('工具结果');
    setDetailContent(JSON.stringify(toolResult ?? null, null, 2));
    setDetailVisible(true);
  };

  const rowClassName = (record: SessionMessage): string => {
    if (!sessionId) {
      return '';
    }
    return record.sessionId === sessionId ? 'conversation-main-row' : 'conversation-child-row';
  };

  const columns: ColumnsType<SessionMessage> = [
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 100,
      render: (role: string) => {
        const config = ROLE_LABELS[role] || { text: role, color: 'default' };
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      render: (content: string, record: SessionMessage) => {
        if (record.role === 'assistant' && record.toolCalls && record.toolCalls.length > 0) {
          const toolCalls = record.toolCalls;
          return (
            <Button size="small" onClick={() => openToolCallsModal(toolCalls)}>
              查看工具 ({toolCalls.length})
            </Button>
          );
        }
        if (record.role === 'tool') {
          return (
            <Button size="small" onClick={() => openToolResultModal(record.toolResult)}>
              查看结果
            </Button>
          );
        }
        return content;
      },
    },
    {
      title: '来源会话',
      dataIndex: 'sessionId',
      key: 'sessionId',
      width: 160,
      render: (sid: string) => (
        <Tooltip title={sid}>
          <span style={{ fontFamily: 'monospace' }}>{shortenSessionId(sid)}</span>
        </Tooltip>
      ),
    },
    {
      title: '时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
  ];

  return (
    <div>
      <Button
        icon={<ArrowLeftOutlined />}
        style={{ marginBottom: 12 }}
        onClick={() => navigate(sessionId ? `/conversations/${sessionId}` : '/conversations')}
      >
        返回
      </Button>
      <Typography.Title level={5} style={{ marginBottom: 16 }}>
        对话详情
      </Typography.Title>
      <Table<SessionMessage>
        rowKey="id"
        columns={columns}
        dataSource={messages}
        loading={loading}
        pagination={false}
        rowClassName={rowClassName}
      />
      <Modal
        title={detailTitle}
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={720}
      >
        <pre
          style={{
            maxHeight: 480,
            overflow: 'auto',
            margin: 0,
            padding: 12,
            backgroundColor: '#f5f5f5',
            borderRadius: 4,
            fontSize: 12,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}
        >
          {detailContent}
        </pre>
      </Modal>
    </div>
  );
}

export default ConversationDetail;