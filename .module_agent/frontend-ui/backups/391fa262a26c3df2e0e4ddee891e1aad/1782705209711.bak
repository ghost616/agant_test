import { useCallback, useEffect, useState } from 'react';
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
  Switch,
  Table,
  Tag,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { CommonStatus } from '../../types/common';
import type { AgentConfig, AgentFormData } from '../../types/agent';
import type { ModelConfig } from '../../types/model';
import type { ToolConfig } from '../../types/tool';
import {
  createAgent,
  deleteAgent,
  listAgents,
  updateAgent,
  updateAgentStatus,
} from '../../services/agent';
import { listModels } from '../../services/model';
import { listTools } from '../../services/tool';

const STATUS_LABELS: Record<CommonStatus, string> = {
  ENABLED: '启用',
  DISABLED: '禁用',
};

const STATUS_OPTIONS = Object.entries(STATUS_LABELS).map(([value, label]) => ({
  value,
  label,
}));

function AgentList(): JSX.Element {
  const [dataSource, setDataSource] = useState<AgentConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchName, setSearchName] = useState('');
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);

  const [modalVisible, setModalVisible] = useState(false);
  const [editingAgent, setEditingAgent] = useState<AgentConfig | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<AgentFormData>();

  const [modelList, setModelList] = useState<ModelConfig[]>([]);
  const [toolList, setToolList] = useState<ToolConfig[]>([]);
  const [modelMap, setModelMap] = useState<Record<string, string>>({});

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listAgents({
        name: searchName || undefined,
        status: filterStatus as CommonStatus | undefined,
      });
      setDataSource(result);
    } catch {
      message.error('获取智能体列表失败');
    } finally {
      setLoading(false);
    }
  }, [searchName, filterStatus]);

  const fetchModelsAndTools = useCallback(async () => {
    try {
      const [models, tools] = await Promise.all([
        listModels({}),
        listTools({}),
      ]);
      setModelList(models);
      setToolList(tools);
      const map: Record<string, string> = {};
      models.forEach((m) => {
        map[m.id] = m.name;
      });
      setModelMap(map);
    } catch {
      message.error('获取模型/工具列表失败');
    }
  }, []);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  useEffect(() => {
    fetchModelsAndTools();
  }, [fetchModelsAndTools]);

  const handleSearch = (value: string): void => {
    setSearchName(value);
  };

  const handleAdd = (): void => {
    setEditingAgent(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: AgentConfig): void => {
    setEditingAgent(record);
    setModalVisible(true);
  };

  useEffect(() => {
    if (!editingAgent || !modalVisible) return;
    form.setFieldsValue({
      name: editingAgent.name,
      description: editingAgent.description,
      systemPrompt: editingAgent.systemPrompt,
      modelId: editingAgent.modelId,
      toolIds: editingAgent.toolIds,
      recentMessageCount: editingAgent.recentMessageCount,
    });
  }, [editingAgent, modalVisible, form]);

  const handleDelete = async (record: AgentConfig): Promise<void> => {
    try {
      await deleteAgent(record.id);
      message.success('删除成功');
      fetchList();
    } catch {
      message.error('删除失败');
    }
  };

  const handleStatusChange = async (
    checked: boolean,
    record: AgentConfig,
  ): Promise<void> => {
    const status: CommonStatus = checked ? 'ENABLED' : 'DISABLED';
    try {
      await updateAgentStatus(record.id, status);
      message.success(status === 'ENABLED' ? '已启用' : '已禁用');
      fetchList();
    } catch {
      message.error('状态更新失败');
    }
  };

  const handleModalOk = async (): Promise<void> => {
    let values: AgentFormData;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    setSubmitting(true);
    try {
      if (editingAgent) {
        await updateAgent(editingAgent.id, values);
        message.success('更新成功');
      } else {
        await createAgent(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchList();
    } catch {
      message.error(editingAgent ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<AgentConfig> = [
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
      title: '关联模型',
      dataIndex: 'modelId',
      width: 140,
      render: (value: string | undefined) =>
        value ? modelMap[value] || '-' : '-',
    },
    {
      title: '最近消息',
      dataIndex: 'recentMessageCount',
      width: 100,
      render: (value: number | undefined) =>
        value === undefined || value === null ? '-' : value,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (value: CommonStatus) => (
        <Tag color={value === 'ENABLED' ? 'green' : 'red'}>
          {STATUS_LABELS[value]}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_: unknown, record: AgentConfig) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除该智能体？"
            onConfirm={() => handleDelete(record)}
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
          <Switch
            checked={record.status === 'ENABLED'}
            onChange={(checked) => handleStatusChange(checked, record)}
          />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="搜索智能体名称"
          allowClear
          style={{ width: 240 }}
          onSearch={handleSearch}
        />
        <Select
          placeholder="状态"
          allowClear
          style={{ width: 120 }}
          options={STATUS_OPTIONS}
          value={filterStatus}
          onChange={(value) => {
            setFilterStatus(value);
          }}
        />
        <Button type="primary" onClick={handleAdd}>
          新增智能体
        </Button>
      </Space>

      <Table<AgentConfig>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 1000 }}
      />

      <Modal
        title={editingAgent ? '编辑智能体' : '新增智能体'}
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
            rules={[{ required: true, message: '请输入智能体名称' }]}
          >
            <Input placeholder="请输入智能体名称" maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入智能体描述" rows={3} maxLength={500} showCount />
          </Form.Item>
          <Form.Item name="systemPrompt" label="系统提示词">
            <Input.TextArea placeholder="请输入系统提示词" rows={4} maxLength={2000} showCount />
          </Form.Item>
          <Form.Item name="modelId" label="关联模型">
            <Select
              placeholder="请选择关联模型"
              allowClear
              showSearch
              optionFilterProp="label"
              options={modelList.map((m) => ({
                value: m.id,
                label: m.name,
              }))}
            />
          </Form.Item>
          <Form.Item name="toolIds" label="挂载工具">
            <Select
              mode="multiple"
              placeholder="请选择挂载工具"
              allowClear
              showSearch
              optionFilterProp="label"
              options={toolList.map((t) => ({
                value: t.id,
                label: t.name,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="recentMessageCount"
            label="最近消息数量"
            initialValue={10}
          >
            <InputNumber
              placeholder="请输入最近消息数量"
              min={1}
              max={100}
              style={{ width: '100%' }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default AgentList;
