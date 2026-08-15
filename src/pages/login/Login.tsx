import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Form, Input, message, Typography } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import type { LoginRequest } from '../../types/user';
import { login } from '../../services/auth';

/**
 * 登录页：登录名 + 密码表单，调用登录接口，
 * 成功后后端写入 HttpOnly Cookie 并跳转主界面。
 */
function Login(): JSX.Element {
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);

  const handleFinish = async (values: LoginRequest): Promise<void> => {
    setSubmitting(true);
    try {
      await login(values);
      message.success('登录成功');
      navigate('/');
    } catch (error) {
      const msg = error instanceof Error ? error.message : '登录失败';
      message.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card style={{ width: 380, boxShadow: '0 2px 8px rgba(0, 0, 0, 0.09)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Typography.Title level={3} style={{ marginBottom: 4 }}>
            Agent 低代码平台
          </Typography.Title>
          <Typography.Text type="secondary">请登录后使用系统</Typography.Text>
        </div>
        <Form<LoginRequest>
          name="loginForm"
          layout="vertical"
          size="large"
          onFinish={handleFinish}
        >
          <Form.Item
            name="loginName"
            label="登录名"
            rules={[{ required: true, message: '请输入登录名' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="请输入登录名"
              autoComplete="username"
              maxLength={50}
            />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入密码"
              autoComplete="current-password"
              maxLength={100}
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" block loading={submitting}>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}

export default Login;
