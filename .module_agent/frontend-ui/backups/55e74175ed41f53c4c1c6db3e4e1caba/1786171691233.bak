import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, message, Table, Tag, Typography } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { SessionMessage } from '../../types/session';
import { getConversationMessages } from '../../services/session';

const ROLE_LABELS: Record<string, { text: string; color: string }> = {
  user: { text: '用户', color: 'blue' },
  assistant: { text: '助手', color: 'green' },
  tool: { text: '工具', color: 'purple' },
  system: { text: '系统', color: 'default' },
};

function ConversationDetail(): JSX.Element {
  const { conversationId } = useParams<{ conversationId?: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const sessionId = (location.state as { sessionId?: string } | null)?.sessionId;
  const [messages, setMessages] = useState<SessionMessage[]>([]);
  const [loading, setLoading] = useState(false);

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
      />
    </div>
  );
}

export default ConversationDetail;