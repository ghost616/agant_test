import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
import type { KBFormData, KnowledgeBase } from '../../types/knowledge';
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  listKnowledgeBases,
  updateKnowledgeBase,
  updateKnowledgeBaseStatus,
} from '../../services/knowledge';

const STATUS_LABELS: Record<CommonStatus, string> = {
  ENABLED: '启用',
  DISABLED: '禁用',
};

const STATUS_OPTIONS = Object.entries(STATUS_LABELS).map(([value, label]) => ({
  value,
  label,
}));

function KnowledgeBaseList(): JSX.Element {
  const navigate = useNavigate();
  const [dataSource, setDataSource] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchName, setSearchName] = useState('');
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);

  const [modalVisible, setModalVisible] = useState(false);
  const [editingKB, setEditingKB] = useState<KnowledgeBase | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<KBFormData>();

  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const result = await listKnowledgeBases({
        name: searchName || undefined,
        status: filterStatus,
      });
      setDataSource(result);
    } catch {
      message.error('获取知识库列表失败');
    } finally {
      setLoading(false);
    }
  }, [searchName, filterStatus]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  const handleSearch = (value: string): void => {
    setSearchName(value);
  };

  const handleAdd = (): void => {
    setEditingKB(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: KnowledgeBase): void => {
    setEditingKB(record);
    setModalVisible(true);
  };

  useEffect(() => {
    if (!editingKB || !modalVisible) return;
    form.setFieldsValue({
      name: editingKB.name,
      description: editingKB.description,
    });
  }, [editingKB, modalVisible, form]);

  const handleDelete = async (record: KnowledgeBase): Promise<void> => {
    try {
      await deleteKnowledgeBase(record.id);
      message.success('删除成功');
      fetchList();
    } catch {
      message.error('删除失败');
    }
  };

  const handleStatusChange = async (
    checked: boolean,
    record: KnowledgeBase,
  ): Promise<void> => {
    const status: CommonStatus = checked ? 'ENABLED' : 'DISABLED';
    try {
      await updateKnowledgeBaseStatus(record.id, status);
      message.success(status === 'ENABLED' ? '已启用' : '已禁用');
      fetchList();
    } catch {
      message.error('状态更新失败');
    }
  };

  const handleModalOk = async (): Promise<void> => {
    let values: KBFormData;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    setSubmitting(true);
    try {
      if (editingKB) {
        await updateKnowledgeBase(editingKB.id, values);
        message.success('更新成功');
      } else {
        await createKnowledgeBase(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchList();
    } catch {
      message.error(editingKB ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<KnowledgeBase> = [
    {
      title: '名称',
      dataIndex: 'name',
      width: 200,
      ellipsis: true,
    },
    {
      title: '描述',
      dataIndex: 'description',
      width: 300,
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      align: 'center',
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
      width: 320,
      render: (_: unknown, record: KnowledgeBase) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            onClick={() => navigate(`/knowledge/${record.id}/files`)}
          >
            管理文件
          </Button>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除该知识库？"
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
          placeholder="搜索知识库名称"
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
          新增知识库
        </Button>
      </Space>

      <Table<KnowledgeBase>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 1100 }}
      />

      <Modal
        title={editingKB ? '编辑知识库' : '新增知识库'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        width={560}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入知识库名称' }]}
          >
            <Input placeholder="请输入知识库名称" maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入知识库描述" rows={3} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default KnowledgeBaseList;
