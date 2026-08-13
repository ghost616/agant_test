import { useCallback, useEffect, useMemo, useState } from 'react';
import type { Key } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, message, Modal, Popconfirm, Space, Spin, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Evaluation, EvaluationResult } from '../../types/evaluation';
import type { ModelConfig } from '../../types/model';
import {
  getEvaluation,
  getEvaluationResults,
  deleteEvaluationResult,
  batchDeleteEvaluationResults,
  clearEvaluationResults,
} from '../../services/evaluation';
import { listModels } from '../../services/model';
import { useEvaluationExecute } from './hooks/useEvaluationExecute';

function EvaluationResultList(): JSX.Element {
  const navigate = useNavigate();
  const { evaluationId } = useParams<{ evaluationId: string }>();
  const [evaluation, setEvaluation] = useState<Evaluation | null>(null);
  const [dataSource, setDataSource] = useState<EvaluationResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [models, setModels] = useState<ModelConfig[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);

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
      const [ev, results, modelList] = await Promise.all([
        getEvaluation(evaluationId),
        getEvaluationResults(evaluationId),
        listModels({}),
      ]);
      setEvaluation(ev);
      setDataSource(results);
      setModels(modelList);
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

  const handleBatchDelete = useCallback((): void => {
    if (selectedRowKeys.length === 0) return;
    Modal.confirm({
      title: '批量删除',
      content: `确定要删除选中的 ${selectedRowKeys.length} 条评估结果吗？`,
      okText: '删除',
      okButtonProps: { danger: true },
      onOk: async (): Promise<void> => {
        try {
          await batchDeleteEvaluationResults(selectedRowKeys.map(String));
          message.success('批量删除成功');
          setSelectedRowKeys([]);
          await fetchData();
        } catch {
          message.error('批量删除失败');
        }
      },
    });
  }, [selectedRowKeys, fetchData]);

  const handleClear = useCallback((): void => {
    if (!evaluationId) return;
    Modal.confirm({
      title: '清空结果',
      content: '确定要清空该评估下的所有评估结果吗？',
      okText: '清空',
      okButtonProps: { danger: true },
      onOk: async (): Promise<void> => {
        try {
          await clearEvaluationResults(evaluationId);
          message.success('清空成功');
          setSelectedRowKeys([]);
          await fetchData();
        } catch {
          message.error('清空失败');
        }
      },
    });
  }, [evaluationId, fetchData]);

  const modelMap = useMemo(() => {
    const map: Record<string, string> = {};
    models.forEach((m) => { map[m.id] = m.name; });
    return map;
  }, [models]);

  const renderScore = (score?: number): React.ReactNode => {
    if (score === undefined || score === null) return '-';
    let color: string;
    if (score < 60) {
      color = 'red';
    } else if (score < 80) {
      color = 'orange';
    } else if (score < 100) {
      color = 'blue';
    } else {
      color = 'green';
    }
    return <Tag color={color}>{score}</Tag>;
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
      title: '模型',
      dataIndex: 'modelId',
      width: 140,
      ellipsis: true,
      render: (val: string) => (val ? (modelMap[val] || val) : '-'),
    },
    {
      title: '最终评分',
      dataIndex: 'finalScore',
      width: 120,
      render: (_: unknown, record: EvaluationResult) => renderScore(record.finalScore),
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
        <Button
          danger
          disabled={selectedRowKeys.length === 0}
          onClick={handleBatchDelete}
        >
          批量删除
        </Button>
        <Button danger onClick={handleClear}>
          清空
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
        scroll={{ x: 1200 }}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys),
        }}
      />

      <Modal
        title="执行日志"
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
