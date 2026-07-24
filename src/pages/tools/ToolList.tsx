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
import JsonEditor from '../../components/JsonEditor';
import type { ColumnsType } from 'antd/es/table';
import type { CommonStatus } from '../../types/common';
import type { SubToolType, ToolConfig, ToolFormData, ToolType } from '../../types/tool';
import {
  createTool,
  deleteTool,
  listTools,
  updateTool,
  updateToolStatus,
} from '../../services/tool';

const TOOL_TYPE_LABELS: Record<ToolType, string> = {
  JAVA: 'Java',
  TYPESCRIPT: 'TypeScript',
  PYTHON: 'Python',
  MCP_HTTP: 'MCP HTTP',
  CUSTOM: 'Custom',
};

const TOOL_TYPE_OPTIONS = Object.entries(TOOL_TYPE_LABELS).map(([value, label]) => ({
  value,
  label,
}));

const TOOL_TYPE_COLORS: Record<ToolType, string> = {
  JAVA: 'orange',
  TYPESCRIPT: 'blue',
  PYTHON: 'green',
  MCP_HTTP: 'purple',
  CUSTOM: 'cyan',
};

const SUB_TOOL_TYPE_LABELS: Record<SubToolType, string> = {
  BROWSER: 'Browser',
};

const SUB_TOOL_TYPE_OPTIONS = Object.entries(SUB_TOOL_TYPE_LABELS).map(([value, label]) => ({
  value,
  label,
}));

const SUB_TOOL_TYPE_COLORS: Record<SubToolType, string> = {
  BROWSER: 'geekblue',
};

const STATUS_LABELS: Record<CommonStatus, string> = {
  ENABLED: '启用',
  DISABLED: '禁用',
};

const STATUS_OPTIONS = Object.entries(STATUS_LABELS).map(([value, label]) => ({
  value,
  label,
}));

function ToolList(): JSX.Element {
  const [dataSource, setDataSource] = useState<ToolConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchName, setSearchName] = useState('');
  const [filterToolType, setFilterToolType] = useState<string | undefined>(undefined);
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);

  const [modalVisible, setModalVisible] = useState(false);
  const [editingTool, setEditingTool] = useState<ToolConfig | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<ToolFormData>();
  const toolType = Form.useWatch('toolType', form);
  const subToolType = Form.useWatch('subToolType', form);

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listTools({
        name: searchName || undefined,
        toolType: filterToolType,
        status: filterStatus,
      });
      setDataSource(result);
    } catch {
      message.error('获取工具列表失败');
    } finally {
      setLoading(false);
    }
  }, [searchName, filterToolType, filterStatus]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  const handleSearch = (value: string): void => {
    setSearchName(value);
  };

  const handleAdd = (): void => {
    setEditingTool(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: ToolConfig): void => {
    setEditingTool(record);
    setModalVisible(true);
  };

  useEffect(() => {
    if (!editingTool || !modalVisible) return;
    const values: Record<string, unknown> = {
      name: editingTool.name,
      toolType: editingTool.toolType,
      description: editingTool.description,
      parameterSchema: editingTool.parameterSchema,
      returnSchema: editingTool.returnSchema,
      implPath: editingTool.implPath,
      subToolType: editingTool.subToolType,
      toolScript: editingTool.toolScript,
    };
    if (editingTool.authConfig) {
      try {
        const auth = JSON.parse(editingTool.authConfig);
        if (auth.token) {
          values.authorization = auth.token;
        }
      } catch {
        // ignore parse error
      }
    }
    form.setFieldsValue(values);
  }, [editingTool, modalVisible, form]);

  const handleDelete = async (record: ToolConfig): Promise<void> => {
    try {
      await deleteTool(record.id);
      message.success('删除成功');
      fetchList();
    } catch {
      message.error('删除失败');
    }
  };

  const handleStatusChange = async (
    checked: boolean,
    record: ToolConfig,
  ): Promise<void> => {
    const status: CommonStatus = checked ? 'ENABLED' : 'DISABLED';
    try {
      await updateToolStatus(record.id, status);
      message.success(status === 'ENABLED' ? '已启用' : '已禁用');
      fetchList();
    } catch {
      message.error('状态更新失败');
    }
  };

  const handleModalOk = async (): Promise<void> => {
    try {
      await form.validateFields();
    } catch {
      return;
    }

    setSubmitting(true);
    try {
      const allValues = form.getFieldsValue();
      const values: ToolFormData = {
        name: allValues.name,
        toolType: allValues.toolType,
        description: allValues.description || '',
        parameterSchema: allValues.parameterSchema || '',
        returnSchema: allValues.returnSchema || '',
        implPath: allValues.implPath,
        subToolType: undefined,
        toolScript: undefined,
      };

      if (allValues.toolType === 'CUSTOM') {
        values.subToolType = allValues.subToolType;
        values.toolScript = allValues.toolScript || '';
        values.implPath = '';
        values.parameterSchema = '';
        values.returnSchema = '';
      } else if (allValues.toolType === 'MCP_HTTP') {
        values.parameterSchema = '';
        values.returnSchema = '';
        const authorization = form.getFieldValue('authorization') as string | undefined;
        if (authorization) {
          values.authConfig = JSON.stringify({ type: 'bearer', token: authorization });
        }
      }

      if (editingTool) {
        await updateTool(editingTool.id, values);
        message.success('更新成功');
      } else {
        await createTool(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchList();
    } catch {
      message.error(editingTool ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<ToolConfig> = [
    {
      title: '名称',
      dataIndex: 'name',
      width: 160,
      ellipsis: true,
    },
    {
      title: '工具类型',
      dataIndex: 'toolType',
      width: 120,
      render: (value: ToolType, record: ToolConfig) => (
        <Space size={4}>
          <Tag color={TOOL_TYPE_COLORS[value]}>{TOOL_TYPE_LABELS[value] || value}</Tag>
          {value === 'CUSTOM' && record.subToolType && (
            <Tag color={SUB_TOOL_TYPE_COLORS[record.subToolType]}>
              {SUB_TOOL_TYPE_LABELS[record.subToolType]}
            </Tag>
          )}
        </Space>
      ),
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
      render: (_: unknown, record: ToolConfig) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除该工具？"
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
          placeholder="搜索工具名称"
          allowClear
          style={{ width: 240 }}
          onSearch={handleSearch}
        />
        <Select
          placeholder="工具类型"
          allowClear
          style={{ width: 140 }}
          options={TOOL_TYPE_OPTIONS}
          value={filterToolType}
          onChange={(value) => {
            setFilterToolType(value);
          }}
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
          新增工具
        </Button>
      </Space>

      <Table<ToolConfig>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 980 }}
      />

      <Modal
        title={editingTool ? '编辑工具' : '新增工具'}
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
            rules={[
              { required: true, message: '请输入工具名称' },
              { pattern: /^[a-z0-9_]+$/, message: '仅允许小写字母、数字和下划线' },
            ]}
          >
            <Input placeholder="请输入工具名称" maxLength={100} />
          </Form.Item>
          <Form.Item
            name="toolType"
            label="工具类型"
            rules={[{ required: true, message: '请选择工具类型' }]}
          >
            <Select options={TOOL_TYPE_OPTIONS} placeholder="请选择工具类型" />
          </Form.Item>
          {toolType === 'MCP_HTTP' && (
            <Form.Item
              name="authorization"
              label="Authorization"
            >
              <Input placeholder="请输入 authorization token" />
            </Form.Item>
          )}
          {toolType === 'CUSTOM' && (
            <Form.Item
              name="subToolType"
              label="子工具类型"
              rules={[{ required: true, message: '请选择子工具类型' }]}
            >
              <Select options={SUB_TOOL_TYPE_OPTIONS} placeholder="请选择子工具类型" />
            </Form.Item>
          )}
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入工具描述" rows={3} maxLength={500} showCount />
          </Form.Item>
          {toolType !== 'MCP_HTTP' && toolType !== 'CUSTOM' && (
            <Form.Item name="parameterSchema" label="参数 Schema">
              <JsonEditor />
            </Form.Item>
          )}
          {toolType !== 'MCP_HTTP' && toolType !== 'CUSTOM' && (
            <Form.Item name="returnSchema" label="返回 Schema">
              <JsonEditor />
            </Form.Item>
          )}
          {toolType === 'CUSTOM' && subToolType === 'BROWSER' ? (
            <Form.Item
              name="toolScript"
              label="工具脚本"
              rules={[{ required: true, message: '请输入工具脚本' }]}
            >
              <Input.TextArea placeholder="请输入工具脚本" rows={6} />
            </Form.Item>
          ) : (
            <Form.Item
              name="implPath"
              label="实现路径"
              rules={[{ required: true, message: '请输入实现路径' }]}
            >
              <Input placeholder="请输入实现路径" />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}

export default ToolList;
