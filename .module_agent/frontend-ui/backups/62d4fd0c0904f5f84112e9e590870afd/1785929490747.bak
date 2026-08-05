import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Evaluation, EvaluationCreateRequest, EvaluationUpdateRequest } from '../../types/evaluation';
import type { ModelConfig } from '../../types/model';
import {
  getEvaluationList,
  createEvaluation,
  updateEvaluation,
  deleteEvaluation,
  clearEvaluationResults,
} from '../../services/evaluation';
import { listModels } from '../../services/model';

function EvaluationList(): JSX.Element {
  const navigate = useNavigate();
  const { agentEvalId: urlAgentEvalId } = useParams<{ agentEvalId: string }>();
  const [dataSource, setDataSource] = useState<Evaluation[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingEvaluation, setEditingEvaluation] = useState<Evaluation | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<EvaluationCreateRequest>();

  const [modelList, setModelList] = useState<ModelConfig[]>([]);

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getEvaluationList(urlAgentEvalId);
      setDataSource(result);
    } catch {
      message.error('获取评估列表失败');
    } finally {
      setLoading(false);
    }
  }, [urlAgentEvalId]);

  const fetchModels = useCallback(async () => {
    try {
      const models = await listModels({});
      setModelList(models);
    } catch {
      message.error('获取模型列表失败');
    }
  }, []);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  useEffect(() => {
    fetchModels();
  }, [fetchModels]);

  const handleAdd = (): void => {
    setEditingEvaluation(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: Evaluation): void => {
    setEditingEvaluation(record);
    setModalVisible(true);
  };

  useEffect(() => {
    if (!editingEvaluation || !modalVisible) return;
    form.setFieldsValue({
      name: editingEvaluation.name,
      description: editingEvaluation.description,
      agentEvalId: editingEvaluation.agentEvalId,
      executionCount: editingEvaluation.executionCount,
      modelId: editingEvaluation.modelId,
      executionType: editingEvaluation.executionType,
    });
  }, [editingEvaluation, modalVisible, form]);

  const handleDelete = async (record: Evaluation): Promise<void> => {
    try {
      await deleteEvaluation(record.id);
      message.success('删除成功');
      fetchList();
    } catch {
      message.error('删除失败');
    }
  };

  const handleClearResults = (record: Evaluation): void => {
    Modal.confirm({
      title: '清空结果',
      content: '确定要清空该评估下的所有评估结果吗？',
      onOk: async () => {
        try {
          await clearEvaluationResults(record.id);
          message.success('清空成功');
          fetchList();
        } catch {
          message.error('清空失败');
        }
      },
    });
  };

  const handleModalOk = async (): Promise<void> => {
    let values: EvaluationCreateRequest;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    setSubmitting(true);
    try {
      if (editingEvaluation) {
        const updateData: EvaluationUpdateRequest = {
          name: values.name,
          description: values.description,
          executionCount: values.executionCount,
          modelId: values.modelId,
          executionType: values.executionType,
        };
        await updateEvaluation(editingEvaluation.id, updateData);
        message.success('更新成功');
      } else {
        await createEvaluation({ ...values, agentEvalId: values.agentEvalId || urlAgentEvalId || '' });
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchList();
    } catch {
      message.error(editingEvaluation ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<Evaluation> = [
    {
      title: '名称',
      dataIndex: 'name',
      width: 160,
      ellipsis: true,
    },
    {
      title: '描述',
      dataIndex: 'description',
      width: 240,
      ellipsis: true,
    },
    {
      title: '智能体',
      dataIndex: 'agentName',
      width: 160,
      ellipsis: true,
      render: (agentName: string, record: Evaluation) => {
        return agentName || record.agentId || '-';
      },
    },
    {
      title: '执行次数',
      dataIndex: 'executionCount',
      width: 100,
    },
    {
      title: '模型',
      dataIndex: 'modelId',
      width: 160,
      ellipsis: true,
      render: (id: string) => {
        const model = modelList.find((m) => m.id === id);
        return model?.name || id;
      },
    },
    {
      title: '执行类型',
      dataIndex: 'executionType',
      width: 100,
      render: (type: string) => {
        if (type === 'BACKGROUND') return '后台执行';
        if (type === 'FOREGROUND') return '前台执行';
        return type || '-';
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'actions',
      width: 440,
      render: (_: unknown, record: Evaluation) => (
        <Space size="small">
           <Button type="link" size="small" onClick={() => handleEdit(record)}>
             修改
           </Button>
          <Button
             type="link"
             size="small"
             onClick={() => navigate(`/evaluations/items/${record.id}/results`)}
           >
             进行评估
           </Button>
          {record.benchmarkSessionId && (
            <Button
              type="link"
              size="small"
              onClick={() => navigate(`/sessions/${record.benchmarkSessionId}/chat?benchmark=1&returnUrl=${encodeURIComponent(`/evaluations/${record.agentEvalId}/items`)}`)}
            >
              基准会话
            </Button>
          )}
          <Button type="link" size="small" danger onClick={() => handleClearResults(record)}>
            清空结果
          </Button>
          <Popconfirm
            title="确定删除该评估？"
            onConfirm={() => handleDelete(record)}
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
      <Space style={{ marginBottom: 16 }}>
        {urlAgentEvalId && (
          <Button onClick={() => navigate('/evaluations')}>
            返回
          </Button>
        )}
        <Button type="primary" onClick={handleAdd}>
          新增评估
        </Button>
      </Space>

      <Table<Evaluation>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 1400 }}
      />

      <Modal
        title={editingEvaluation ? '修改评估' : '新增评估'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入评估名称' }]}
          >
            <Input placeholder="请输入评估名称" maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入评估描述" rows={3} maxLength={500} showCount />
          </Form.Item>
          <Form.Item
            name="modelId"
            label="模型"
            rules={[{ required: true, message: '请选择模型' }]}
          >
            <Select
              placeholder="请选择模型"
              allowClear
              showSearch
              optionFilterProp="label"
              options={modelList.map((m) => ({
                value: m.id,
                label: m.name,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="executionType"
            label="执行类型"
            initialValue="BACKGROUND"
          >
            <Select
              placeholder="请选择执行类型"
              options={[
                { label: '后台执行', value: 'BACKGROUND' },
                { label: '前台执行', value: 'FOREGROUND' },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="executionCount"
            label="执行次数"
            rules={[{ required: true, message: '请输入执行次数' }]}
          >
            <InputNumber
              placeholder="请输入执行次数"
              min={1}
              style={{ width: '100%' }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default EvaluationList;
