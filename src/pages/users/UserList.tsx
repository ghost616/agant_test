import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Result,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { USER_TYPE_ADMIN } from '../../types/user';
import type { User, UserCreateRequest, UserUpdateRequest } from '../../types/user';
import { createUser, listUsers, updateUser } from '../../services/user';
import { getCurrentUser } from '../../services/auth';
import useTableScrollY from '../../hooks/useTableScrollY';

const PAGE_SIZE_OPTIONS = [10, 20, 50];

interface UserFormData {
  loginName?: string;
  displayName?: string;
  password?: string;
}

/**
 * 用户管理页：分页展示用户列表，支持添加/修改用户与禁止/恢复登录，
 * 仅管理员可见可操作（前端按当前登录用户类型拦截，后端接口同样强制校验）。
 */
function UserList(): JSX.Element {
  const scrollY = useTableScrollY(272);
  const isAdmin = getCurrentUser()?.userType === USER_TYPE_ADMIN;

  const [dataSource, setDataSource] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<UserFormData>();

  const fetchList = useCallback(async (): Promise<void> => {
    setLoading(true);
    try {
      const result = await listUsers({ page, size: pageSize });
      setDataSource(result.list);
      setTotal(result.total);
    } catch {
      message.error('获取用户列表失败');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize]);

  useEffect(() => {
    if (isAdmin) {
      fetchList();
    }
  }, [fetchList, isAdmin]);

  const handleAdd = (): void => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: User): void => {
    setEditingUser(record);
    form.setFieldsValue({
      displayName: record.displayName,
    });
    setModalVisible(true);
  };

  const handleModalOk = async (): Promise<void> => {
    let values: UserFormData;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      if (editingUser) {
        const payload: UserUpdateRequest = {
          displayName: values.displayName,
          password: values.password || undefined,
        };
        await updateUser(editingUser.id, payload);
        message.success('修改成功');
      } else {
        const payload: UserCreateRequest = {
          loginName: values.loginName as string,
          displayName: values.displayName,
          password: values.password as string,
        };
        await createUser(payload);
        message.success('添加成功');
      }
      setModalVisible(false);
      fetchList();
    } catch {
      message.error(editingUser ? '修改失败' : '添加失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleLogin = async (record: User): Promise<void> => {
    const enabling = record.enabled !== 1;
    try {
      await updateUser(record.id, { enabled: enabling ? 1 : 0 });
      message.success(enabling ? '已恢复登录' : '已禁止登录');
      fetchList();
    } catch {
      message.error(enabling ? '恢复登录失败' : '禁止登录失败');
    }
  };

  if (!isAdmin) {
    return (
      <Result
        status="403"
        title="无权限访问"
        subTitle="用户管理仅对管理员开放，请联系管理员分配权限。"
      />
    );
  }

  const columns: ColumnsType<User> = [
    {
      title: '登录名',
      dataIndex: 'loginName',
      width: 160,
      ellipsis: true,
    },
    {
      title: '显示名',
      dataIndex: 'displayName',
      width: 160,
      ellipsis: true,
      render: (value?: string) => value || '-',
    },
    {
      title: '登录状态',
      dataIndex: 'enabled',
      width: 120,
      render: (value: number) => (
        <Tag color={value === 1 ? 'green' : 'red'}>
          {value === 1 ? '允许登录' : '禁止登录'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      render: (value?: string) => value || '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_: unknown, record: User) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            修改
          </Button>
          <Popconfirm
            title={record.enabled === 1 ? '确定禁止该用户登录？' : '确定恢复该用户登录？'}
            onConfirm={() => handleToggleLogin(record)}
          >
            <Button
              type="link"
              size="small"
              danger={record.enabled === 1}
            >
              {record.enabled === 1 ? '禁止登录' : '恢复登录'}
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Typography.Title level={4} style={{ margin: 0 }}>
          用户管理
        </Typography.Title>
        <Button type="primary" onClick={handleAdd}>
          添加用户
        </Button>
      </div>

      <Table<User>
        rowKey="id"
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        scroll={{ x: 940, y: scrollY }}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: PAGE_SIZE_OPTIONS,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (nextPage, nextPageSize) => {
            setPage(nextPageSize !== pageSize ? 1 : nextPage);
            setPageSize(nextPageSize);
          },
        }}
      />

      <Modal
        title={editingUser ? '修改用户' : '添加用户'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        width={480}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          {!editingUser && (
            <Form.Item
              name="loginName"
              label="登录名"
              rules={[{ required: true, message: '请输入登录名' }]}
            >
              <Input placeholder="请输入登录名" maxLength={50} />
            </Form.Item>
          )}
          <Form.Item name="displayName" label="显示名">
            <Input placeholder="请输入显示名" maxLength={50} />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={editingUser ? [] : [{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              placeholder={editingUser ? '留空则不修改密码' : '请输入密码'}
              maxLength={100}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default UserList;
