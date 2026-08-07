import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Tag,
} from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { CommonStatus } from '../../types/common';
import type {
  KFFormData,
  KnowledgeBase,
  KnowledgeFile,
  PublishStatus,
} from '../../types/knowledge';
import {
  createKnowledgeFile,
  deleteKnowledgeFile,
  getKnowledgeBase,
  listKnowledgeFiles,
  publishKnowledgeFile,
  refreshKnowledgeFiles,
  updateKnowledgeFile,
  updateKnowledgeFileStatus,
} from '../../services/knowledge';

const STATUS_LABELS: Record<CommonStatus, string> = {
  ENABLED: '启用',
  DISABLED: '禁用',
};

const PUBLISH_STATUS_LABELS: Record<PublishStatus, string> = {
  UNPUBLISHED: '未发布',
  PUBLISHING: '发布中',
  PUBLISHED: '已发布',
  PENDING_PUBLISH: '待发布',
  PUBLISH_ERROR: '发布失败',
};

const PUBLISH_STATUS_COLORS: Record<PublishStatus, string> = {
  UNPUBLISHED: 'default',
  PUBLISHING: 'processing',
  PUBLISHED: 'success',
  PENDING_PUBLISH: 'warning',
  PUBLISH_ERROR: 'error',
};

const PUBLISHABLE_STATUSES: PublishStatus[] = [
  'UNPUBLISHED',
  'PENDING_PUBLISH',
  'PUBLISH_ERROR',
];

function KnowledgeFileList(): JSX.Element {
  const navigate = useNavigate();
  const { kbId } = useParams<{ kbId: string }>();
  const [dataSource, setDataSource] = useState<KnowledgeFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [kb, setKb] = useState<KnowledgeBase | null>(null);
  const [publishingId, setPublishingId] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const [modalVisible, setModalVisible] = useState(false);
  const [editingKF, setEditingKF] = useState<KnowledgeFile | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<KFFormData>();

  const fetchList = useCallback(async () => {
    if (!kbId) return;
    setLoading(true);
    try {
      const result = await listKnowledgeFiles(kbId);
      setDataSource(result);
    } catch {
      message.error('获取文件列表失败');
    } finally {
      setLoading(false);
    }
  }, [kbId]);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  useEffect(() => {
    if (!kbId) return;
    getKnowledgeBase(kbId)
      .then((result) => setKb(result))
      .catch(() => setKb(null));
  }, [kbId]);

  const handleAdd = (): void => {
    setEditingKF(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: KnowledgeFile): void => {
    setEditingKF(record);
    setModalVisible(true);
  };

  useEffect(() => {
    if (!editingKF || !modalVisible) return;
    form.setFieldsValue({
      fileName: editingKF.fileName,
      fileDescription: editingKF.fileDescription,
    });
  }, [editingKF, modalVisible, form]);

  const handleDelete = async (record: KnowledgeFile): Promise<void> => {
    if (!kbId) return;
    try {
      await deleteKnowledgeFile(kbId, record.id);
      message.success('删除成功');
      fetchList();
    } catch {
      message.error('删除失败');
    }
  };

  const handleStatusChange = async (
    checked: boolean,
    record: KnowledgeFile,
  ): Promise<void> => {
    if (!kbId) return;
    const status: CommonStatus = checked ? 'ENABLED' : 'DISABLED';
    try {
      await updateKnowledgeFileStatus(kbId, record.id, status);
      message.success(status === 'ENABLED' ? '已启用' : '已禁用');
      fetchList();
    } catch {
      message.error('状态更新失败');
    }
  };

  const handlePublish = async (record: KnowledgeFile): Promise<void> => {
    if (!kbId) return;
    setPublishingId(record.id);
    try {
      await publishKnowledgeFile(kbId, record.id);
      message.success('发布成功');
      fetchList();
    } catch {
      message.error('发布失败');
    } finally {
      setPublishingId(null);
    }
  };

  const handleRefresh = async (): Promise<void> => {
    if (!kbId) return;
    setRefreshing(true);
    try {
      await refreshKnowledgeFiles(kbId);
      message.success('文件列表已刷新');
      fetchList();
    } catch {
      message.error('刷新文件列表失败');
    } finally {
      setRefreshing(false);
    }
  };

  const handleModalOk = async (): Promise<void> => {
    let values: KFFormData;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    if (!kbId) return;

    setSubmitting(true);
    try {
      if (editingKF) {
        await updateKnowledgeFile(kbId, editingKF.id, values);
        message.success('更新成功');
      } else {
        await createKnowledgeFile(kbId, values);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchList();
    } catch {
      message.error(editingKF ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns: ColumnsType<KnowledgeFile> = [
    {
      title: '文件名',
      dataIndex: 'fileName',
      width: 200,
      ellipsis: true,
    },
    {
      title: '描述',
      dataIndex: 'fileDescription',
      width: 260,
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
      title: '发布状态',
      dataIndex: 'publishStatus',
      width: 120,
      align: 'center',
      render: (value?: PublishStatus) => {
        const status: PublishStatus = value ?? 'UNPUBLISHED';
        return (
          <Tag color={PUBLISH_STATUS_COLORS[status]}>
            {PUBLISH_STATUS_LABELS[status]}
          </Tag>
        );
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
      width: 420,
      render: (_: unknown, record: KnowledgeFile) => {
        const publishStatus: PublishStatus = record.publishStatus ?? 'UNPUBLISHED';
        const isPublishing = publishStatus === 'PUBLISHING';
        const canPublish =
          !kb?.rebuilding && PUBLISHABLE_STATUSES.includes(publishStatus);
        return (
          <Space size="small">
            <Button
              type="link"
              size="small"
              disabled={!canPublish}
              loading={publishingId === record.id}
              onClick={() => handlePublish(record)}
            >
              {isPublishing ? '发布中' : '发布'}
            </Button>
            <Button
              type="link"
              size="small"
              onClick={() => navigate(`/knowledge/${kbId}/files/${record.id}/edit`)}
            >
              编辑内容
            </Button>
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              编辑
            </Button>
            <Popconfirm
              title="确定删除该文件？"
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
        );
      },
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }} wrap>
        <Button type="text" onClick={() => navigate('/knowledge')}>
          返回
        </Button>
        <Button icon={<ReloadOutlined />} loading={refreshing} onClick={handleRefresh}>
          刷新
        </Button>
        <Button type="primary" onClick={handleAdd}>
          新增文件
        </Button>
      </Space>

      <Table<KnowledgeFile>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        pagination={false}
        scroll={{ x: 1300 }}
      />

      <Modal
        title={editingKF ? '编辑文件' : '新增文件'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        width={680}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            name="fileName"
            label="文件名"
            rules={[{ required: true, message: '请输入文件名' }]}
          >
            <Input placeholder="请输入文件名" maxLength={255} />
          </Form.Item>
          <Form.Item name="fileDescription" label="描述">
            <Input.TextArea placeholder="请输入文件描述" rows={2} maxLength={500} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default KnowledgeFileList;
