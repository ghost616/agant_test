import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, message, Space, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Evaluation, EvaluationResult } from '../../types/evaluation';
import { getEvaluation, getEvaluationResults } from '../../services/evaluation';

function EvaluationResultList(): JSX.Element {
  const navigate = useNavigate();
  const { evaluationId } = useParams<{ evaluationId: string }>();
  const [evaluation, setEvaluation] = useState<Evaluation | null>(null);
  const [dataSource, setDataSource] = useState<EvaluationResult[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    if (!evaluationId) return;
    setLoading(true);
    try {
      const [ev, results] = await Promise.all([
        getEvaluation(evaluationId),
        getEvaluationResults(evaluationId),
      ]);
      setEvaluation(ev);
      setDataSource(results);
    } catch {
      message.error('获取评估结果失败');
    } finally {
      setLoading(false);
    }
  }, [evaluationId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleExecute = (): void => {
    message.info('功能开发中');
  };

  const columns: ColumnsType<EvaluationResult> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 200,
      ellipsis: true,
    },
    {
      title: '会话ID',
      dataIndex: 'evaluationSessionId',
      width: 200,
      ellipsis: true,
    },
    {
      title: 'Token消耗',
      dataIndex: 'totalTokenUsed',
      width: 120,
      render: (val: string) => val || '-',
    },
    {
      title: '结果摘要',
      dataIndex: 'result',
      width: 300,
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: () => (
        <Button type="link" size="small" onClick={() => message.info('功能开发中')}>
          修改
        </Button>
      ),
    },
  ];

  return (
    <div>
      <h3 style={{ marginBottom: 16 }}>
        评估名称：{evaluation?.name || '-'}
      </h3>
      <Space style={{ marginBottom: 16 }}>
        <Button
          onClick={() =>
            navigate(
              evaluation?.agentEvalId
                ? `/evaluations/${evaluation.agentEvalId}/items`
                : '/evaluations',
            )
          }
        >
          回退
        </Button>
        <Button type="primary" onClick={handleExecute}>
          执行
        </Button>
      </Space>

      <Table<EvaluationResult>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 1200 }}
      />
    </div>
  );
}

export default EvaluationResultList;
