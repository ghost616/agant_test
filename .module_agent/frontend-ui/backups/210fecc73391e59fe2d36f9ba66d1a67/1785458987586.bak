import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, message, Modal, Popconfirm, Space, Spin, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Evaluation, EvaluationResult } from '../../types/evaluation';
import {
  getEvaluation,
  getEvaluationResults,
  deleteEvaluationResult,
} from '../../services/evaluation';
import { useEvaluationExecute } from './hooks/useEvaluationExecute';

function EvaluationResultList(): JSX.Element {
  const navigate = useNavigate();
  const { evaluationId } = useParams<{ evaluationId: string }>();
  const [evaluation, setEvaluation] = useState<Evaluation | null>(null);
  const [dataSource, setDataSource] = useState<EvaluationResult[]>([]);
  const [loading, setLoading] = useState(false);

  const {
    execute,
    executing,
    executionProgress,
    foregroundModalVisible,
    foregroundLog,
    foregroundLogRef,
    handleCancelForeground,
  } = useEvaluationExecute();

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

  const handleExecute = useCallback(async (): Promise<void> => {
    if (!evaluationId || !evaluation) return;
    await execute(evaluationId, evaluation, fetchData);
  }, [evaluationId, evaluation, execute, fetchData]);

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
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_: unknown, record: EvaluationResult) => (
        <Space>
          <Button type="link" size="small" onClick={() => navigate(`/evaluations/results/${record.id}`)}>
            查看结果
          </Button>
          <Popconfirm
            title="确认删除"
            description="确定要删除该评估结果吗？"
            onConfirm={async () => {
              try {
                await deleteEvaluationResult(record.id);
                message.success('删除成功');
                await fetchData();
              } catch {
                message.error('删除失败');
              }
            }}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
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
          返回
        </Button>
        <Button
          type="primary"
          onClick={handleExecute}
          loading={executing}
          disabled={executing}
        >
          执行
        </Button>
        {executionProgress && (
          <span style={{ color: '#888' }}>{executionProgress}</span>
        )}
      </Space>

      <Table<EvaluationResult>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 900 }}
      />

      <Modal
        title="前台执行"
        open={foregroundModalVisible}
        onCancel={handleCancelForeground}
        footer={
          <Button danger onClick={handleCancelForeground}>
            停止执行
          </Button>
        }
        width={700}
      >
        <Spin spinning={executing}>
          <div
            ref={foregroundLogRef}
            style={{
              background: '#1e1e1e',
              color: '#d4d4d4',
              padding: 12,
              borderRadius: 4,
              maxHeight: 400,
              overflow: 'auto',
              fontFamily: 'monospace',
              fontSize: 13,
            }}
          >
            {foregroundLog.map((line, i) => (
              <div key={i}>{line}</div>
            ))}
          </div>
        </Spin>
      </Modal>
    </div>
  );
}

export default EvaluationResultList;
