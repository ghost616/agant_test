import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, message, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Session } from '../../types/session';
import { listLogSessions } from '../../services/session';
import useTableScrollY from '../../hooks/useTableScrollY';

/**
 * 会话日志页：展示当前用户的所有主会话（含评估会话），
 * 每条提供「查看日志」按钮跳转 /logs/{sessionId} 进入该主会话的运行日志页。
 */
function SessionLogList(): JSX.Element {
  const navigate = useNavigate();
  const scrollY = useTableScrollY(216);

  const [dataSource, setDataSource] = useState<Session[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchList = useCallback(async (): Promise<void> => {
    setLoading(true);
    try {
      const result = await listLogSessions();
      setDataSource(result);
    } catch {
      message.error('获取会话日志列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  const columns: ColumnsType<Session> = [
    {
      title: '会话名',
      dataIndex: 'title',
      width: 240,
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: '是否评估',
      dataIndex: 'isEvaluation',
      width: 140,
      render: (value?: boolean) => (
        <Tag color={value ? 'gold' : 'default'}>{value ? '评估会话' : '普通会话'}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      render: (value?: string) => value || '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 160,
      render: (_: unknown, record: Session) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => navigate(`/logs/${record.id}`)}>
            查看日志
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Typography.Title level={4} style={{ margin: 0 }}>
          会话日志
        </Typography.Title>
      </div>

      <Table<Session>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 720, y: scrollY }}
      />
    </div>
  );
}

export default SessionLogList;