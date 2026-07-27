import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
import type { Session } from '../../types/session';
import type { ModelConfig } from '../../types/model';
import {
  getEvaluationList,
  createEvaluation,
  updateEvaluation,
  deleteEvaluation,
} from '../../services/evaluation';
import { listSessions } from '../../services/session';
import { listModels } from '../../services/model';

function EvaluationList(): JSX.Element {
  const navigate = useNavigate();
  const [dataSource, setDataSource] = useState<Evaluation[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingEvaluation, setEditingEvaluation] = useState<Evaluation | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<EvaluationCreateRequest>();

  const [sessionList, setSessionList] = useState<Session[]>([]);
  const [modelList, setModelList] = useState<ModelConfig[]>([]);

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getEvaluationList();
      setDataSource(result);
    } catch {
      message.error('获取评估列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchRefData = useCallback(async () => {
    try {
      const [sessions, models] = await Promise.all([
        listSessions(),
        listModels({}),
      ]);
      setSessionList(sessions);
      setModelList(models);
    } catch {
      message.error('获取会话/模型列表失败');
    }
  }, []);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  useEffect(() => {
    fetchRefData();
  }, [fetchRefData]);

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
      benchmarkSessionId: editingEvaluation.benchmarkSessionId,
      executionCount: editingEvaluation.executionCount,
      modelId: editingEvaluation.modelId,
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
          benchmarkSessionId: values.benchmarkSessionId,
          executionCount: values.executionCount,
          modelId: values.modelId,
        };
        await updateEvaluation(editingEvaluation.id, updateData);
        message.success('更新成功');
      } else {
        await createEvaluation(values);
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
      title: '基准会话',
      dataIndex: 'benchmarkSessionId',
      width: 200,
      ellipsis: true,
      render: (id: string) => {
        const session = sessionList.find((s) => s.id === id);
        return session?.title || id;
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
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'actions',
      width: 260,
      render: (_: unknown, record: Evaluation) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            修改
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => navigate(`/evaluations/${record.id}/results`)}
          >
            进行评估
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
        scroll={{ x: 1200 }}
      />

      <Modal
        title={editingEvaluation ? '修改评估' : '新增评估'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        width={640}
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
            name="benchmarkSessionId"
            label="基准会话"
            rules={[{ required: true, message: '请选择基准会话' }]}
          >
            <Select
              placeholder="请选择基准会话"
              allowClear
              showSearch
              optionFilterProp="label"
              options={sessionList.map((s) => ({
                value: s.id,
                label: s.title || s.id,
              }))}
            />
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
