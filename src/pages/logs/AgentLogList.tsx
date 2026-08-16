import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Input, message, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { AgentLog, AgentLogQueryParams } from '../../types/log';
import { LogLevel, LogType } from '../../types/log';
import { listAgentLogs } from '../../services/log';
import useTableScrollY from '../../hooks/useTableScrollY';

const PAGE_SIZE_OPTIONS = [20, 50, 100];

const LOG_TYPE_LABELS: Record<string, string> = {};
Object.values(LogType).forEach((item) => {
  LOG_TYPE_LABELS[item.code] = item.label;
});

const LOG_TYPE_OPTIONS = Object.values(LogType).map((item) => ({
  value: item.code,
  label: item.label,
}));

const LOG_LEVEL_COLORS: Record<string, string> = {
  INFO: '#1677ff',
  ERROR: '#ff4d4f',
};

const LOG_LEVEL_OPTIONS = Object.values(LogLevel).map((item) => ({
  value: item.code,
  label: item.code,
}));

const LOG_DATA_PREVIEW_LENGTH = 60;

/**
 * 格式化日志数据：合法 JSON 时美化缩进展示，否则原样返回。
 * @param data 原始日志数据
 * @returns 格式化后的展示文本
 */
function formatLogData(data: string): string {
  try {
    return JSON.stringify(JSON.parse(data), null, 2);
  } catch {
    return data;
  }
}

function AgentLogList(): JSX.Element {
  // 从路由参数读取主会话 sessionId，日志查询按该主会话及其子会话过滤
  const { sessionId } = useParams<{ sessionId: string }>();
  const scrollY = useTableScrollY(272);
  const [dataSource, setDataSource] = useState<AgentLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchName, setSearchName] = useState('');
  const [filterLogType, setFilterLogType] = useState<string | undefined>(undefined);
  const [filterLogLevel, setFilterLogLevel] = useState<string | undefined>(undefined);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [detailLog, setDetailLog] = useState<AgentLog | null>(null);
  const [detailVariable, setDetailVariable] = useState<{ title: string; content: string } | null>(
    null,
  );

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const params: AgentLogQueryParams = {
        sessionName: searchName || undefined,
        logType: filterLogType,
        logLevel: filterLogLevel,
        page,
        size: pageSize,
      };
      if (sessionId) {
        params.rootSessionId = sessionId;
      }
      const result = await listAgentLogs(params);
      setDataSource(result.list);
      setTotal(result.total);
    } catch {
      message.error('获取日志列表失败');
    } finally {
      setLoading(false);
    }
  }, [searchName, filterLogType, filterLogLevel, page, pageSize, sessionId]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  const handleSearch = (value: string): void => {
    setSearchName(value);
    setPage(1);
  };

  const handleLogTypeChange = (value: string | undefined): void => {
    setFilterLogType(value);
    setPage(1);
  };

  const handleLogLevelChange = (value: string | undefined): void => {
    setFilterLogLevel(value);
    setPage(1);
  };

  const renderExpandableText = (value: string, onExpand: () => void): React.ReactNode => {
    const truncated = value.length > LOG_DATA_PREVIEW_LENGTH;
    const preview = truncated
      ? `${value.slice(0, LOG_DATA_PREVIEW_LENGTH)}...`
      : value;
    return (
      <Space size={4} align="start">
        <Typography.Text
          style={{ color: '#666', wordBreak: 'break-all', whiteSpace: 'pre-wrap' }}
        >
          {preview}
        </Typography.Text>
        {truncated && (
          <Button type="link" size="small" onClick={onExpand}>
            展开
          </Button>
        )}
      </Space>
    );
  };

  const renderVariableCell = (value: string | undefined, title: string): React.ReactNode => {
    if (!value) {
      return '-';
    }
    return renderExpandableText(value, () => setDetailVariable({ title, content: value }));
  };

  const columns: ColumnsType<AgentLog> = [
    {
      title: '会话名',
      dataIndex: 'sessionName',
      width: 160,
      ellipsis: true,
      render: (value?: string, record?: AgentLog) => value || record?.sessionId || '-',
    },
    {
      title: '会话类型',
      dataIndex: 'isChild',
      width: 110,
      render: (value?: boolean) => (
        <Tag color={value ? 'blue' : 'default'}>{value ? '子会话' : '主会话'}</Tag>
      ),
    },
    {
      title: '对话ID',
      dataIndex: 'conversationId',
      width: 200,
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: '日志类型',
      dataIndex: 'logType',
      width: 140,
      render: (value: string) => (
        <Tag color="geekblue">{LOG_TYPE_LABELS[value] || value}</Tag>
      ),
    },
    {
      title: '日志等级',
      dataIndex: 'logLevel',
      width: 110,
      render: (value: string) => (
        <span style={{ color: LOG_LEVEL_COLORS[value], fontWeight: 500 }}>{value}</span>
      ),
    },
    {
      title: '日志数据',
      dataIndex: 'logData',
      width: 360,
      render: (value: string, record: AgentLog) => renderExpandableText(value, () => setDetailLog(record)),
    },
    {
      title: '会话变量',
      dataIndex: 'sessionVariables',
      width: 240,
      render: (value?: string) => renderVariableCell(value, '会话变量'),
    },
    {
      title: '对话变量',
      dataIndex: 'conversationVariables',
      width: 240,
      render: (value?: string) => renderVariableCell(value, '对话变量'),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="搜索会话名"
          allowClear
          style={{ width: 240 }}
          onSearch={handleSearch}
        />
        <Select
          placeholder="日志类型"
          allowClear
          style={{ width: 160 }}
          options={LOG_TYPE_OPTIONS}
          value={filterLogType}
          onChange={handleLogTypeChange}
        />
        <Select
          placeholder="日志等级"
          allowClear
          style={{ width: 120 }}
          options={LOG_LEVEL_OPTIONS}
          value={filterLogLevel}
          onChange={handleLogLevelChange}
        />
      </Space>

      <Table<AgentLog>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        scroll={{ x: 1740, y: scrollY }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: PAGE_SIZE_OPTIONS,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (nextPage, nextPageSize) => {
            setPage(nextPageSize !== pageSize ? 1 : nextPage);
            setPageSize(nextPageSize);
          },
        }}
      />

      <Modal
        title="日志详情"
        open={detailLog !== null}
        onCancel={() => setDetailLog(null)}
        footer={
          <Button type="primary" onClick={() => setDetailLog(null)}>
            关闭
          </Button>
        }
        width={720}
        destroyOnHidden
      >
        {detailLog && (
          <div>
            <Space direction="vertical" size={4} style={{ marginBottom: 12 }}>
              <span>
                会话名：{detailLog.sessionName || '-'}　对话ID：
                {detailLog.conversationId || '-'}
              </span>
              <span>
                日志类型：{LOG_TYPE_LABELS[detailLog.logType] || detailLog.logType}　
                日志等级：
                <span
                  style={{ color: LOG_LEVEL_COLORS[detailLog.logLevel], fontWeight: 500 }}
                >
                  {detailLog.logLevel}
                </span>
              </span>
              <span>创建时间：{detailLog.createTime}</span>
            </Space>
            <pre
              style={{
                background: '#1e1e1e',
                color: '#d4d4d4',
                padding: 12,
                borderRadius: 4,
                maxHeight: 480,
                overflow: 'auto',
                fontFamily: 'monospace',
                fontSize: 13,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
              }}
            >
              {formatLogData(detailLog.logData)}
            </pre>
          </div>
        )}
      </Modal>

      <Modal
        title={detailVariable ? `${detailVariable.title}详情` : '变量详情'}
        open={detailVariable !== null}
        onCancel={() => setDetailVariable(null)}
        footer={
          <Button type="primary" onClick={() => setDetailVariable(null)}>
            关闭
          </Button>
        }
        width={720}
        destroyOnHidden
      >
        {detailVariable && (
          <pre
            style={{
              background: '#1e1e1e',
              color: '#d4d4d4',
              padding: 12,
              borderRadius: 4,
              maxHeight: 480,
              overflow: 'auto',
              fontFamily: 'monospace',
              fontSize: 13,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
            }}
          >
            {formatLogData(detailVariable.content)}
          </pre>
        )}
      </Modal>
    </div>
  );
}

export default AgentLogList;
