import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
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
import type { SkillConfig, SkillFormData } from '../../types/skill';
import type { ToolConfig } from '../../types/tool';
import {
  createSkill,
  deleteSkill,
  listSkills,
  updateSkill,
  updateSkillStatus,
} from '../../services/skill';
import { listTools } from '../../services/tool';

const STATUS_LABELS: Record<CommonStatus, string> = {
  ENABLED: '启用',
  DISABLED: '禁用',
};

const STATUS_OPTIONS = Object.entries(STATUS_LABELS).map(([value, label]) => ({
  value,
  label,
}));

function SkillList(): JSX.Element {
  const [dataSource, setDataSource] = useState<SkillConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchName, setSearchName] = useState('');
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);

  const [modalVisible, setModalVisible] = useState(false);
  const [editingSkill, setEditingSkill] = useState<SkillConfig | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<SkillFormData>();

  const [toolList, setToolList] = useState<ToolConfig[]>([]);

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listSkills({
        name: searchName || undefined,
        status: filterStatus as CommonStatus | undefined,
      });
      setDataSource(result);
    } catch {
      message.error('获取技能列表失败');
    } finally {
      setLoading(false);
    }
  }, [searchName, filterStatus]);

  const fetchTools = useCallback(async () => {
    try {
      const tools = await listTools({});
      setToolList(tools);
    } catch {
      message.error('获取工具列表失败');
    }
  }, []);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  useEffect(() => {
    fetchTools();
  }, [fetchTools]);

  const handleSearch = (value: string): void => {
    setSearchName(value);
  };

  const handleAdd = (): void => {
    setEditingSkill(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: SkillConfig): void => {
    setEditingSkill(record);
    setModalVisible(true);
  };

  useEffect(() => {
    if (!editingSkill || !modalVisible) return;
    form.setFieldsValue({
      name: editingSkill.name,
      description: editingSkill.description,
      prompt: editingSkill.prompt,
      toolIds: editingSkill.toolIds,
    });
  }, [editingSkill, modalVisible, form]);

  const handleDelete = async (record: SkillConfig): Promise<void> => {
    try {
      await deleteSkill(record.id);
      message.success('删除成功');
      fetchList();
    } catch {
      message.error('删除失败');
    }
  };

  const handleStatusChange = async (
    checked: boolean,
    record: SkillConfig,
  ): Promise<void> => {
    const status: CommonStatus = checked ? 'ENABLED' : 'DISABLED';
    try {
      await updateSkillStatus(record.id, status);
      message.success(status === 'ENABLED' ? '已启用' : '已禁用');
      fetchList();
    } catch {
      message.error('状态更新失败');
    }
  };

  const handleModalOk = async (): Promise<void> => {
    let values: SkillFormData;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    setSubmitting(true);
    try {
      if (editingSkill) {
        await updateSkill(editingSkill.id, values);
        message.success('更新成功');
      } else {
        await createSkill(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchList();
    } catch {
      message.error(editingSkill ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<SkillConfig> = [
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
      render: (_: unknown, record: SkillConfig) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除该技能？"
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
          placeholder="搜索技能名称"
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
          新增技能
        </Button>
      </Space>

      <Table<SkillConfig>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 1000 }}
      />

      <Modal
        title={editingSkill ? '编辑技能' : '新增技能'}
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
            rules={[{ required: true, message: '请输入技能名称' }]}
          >
            <Input placeholder="请输入技能名称" maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input placeholder="请输入技能描述" maxLength={200} />
          </Form.Item>
          <Form.Item
            name="prompt"
            label="提示词"
            rules={[{ required: true, message: '请输入技能提示词' }]}
          >
            <Input.TextArea placeholder="请输入技能提示词" rows={6} maxLength={5000} showCount />
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
        </Form>
      </Modal>
    </div>
  );
}

export default SkillList;
