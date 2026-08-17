import { useEffect, useState } from 'react';
import { Navigate, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Avatar, Dropdown, Form, Input, Layout, Menu, message, Modal, Space } from 'antd';
import type { MenuProps } from 'antd';
import { getCurrentUser, logout, saveCurrentUser } from './services/auth';
import { webSocketClient } from './services/websocket';
import { updateCurrentUser } from './services/user';
import { USER_TYPE_ADMIN } from './types/user';
import type { User } from './types/user';
import {
  ApiOutlined,
  BookOutlined,
  CheckCircleOutlined,
  EyeOutlined,
  FileTextOutlined,
  HistoryOutlined,
  MessageOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons';
import ModelList from './pages/models/ModelList';
import ModelTest from './pages/models/ModelTest';
import ToolList from './pages/tools/ToolList';
import AgentList from './pages/agents/AgentList';
import SessionList from './pages/sessions/SessionList';
import AgentChat from './pages/sessions/AgentChat';
import ConversationHistory from './pages/sessions/ConversationHistory';
import ConversationDetail from './pages/sessions/ConversationDetail';
import SkillList from './pages/skills/SkillList';
import KnowledgeBaseList from './pages/knowledge/KnowledgeBaseList';
import KnowledgeFileList from './pages/knowledge/KnowledgeFileList';
import KnowledgeFileEdit from './pages/knowledge/KnowledgeFileEdit';
import AgentEvaluationList from './pages/evaluations/AgentEvaluationList';
import EvaluationList from './pages/evaluations/EvaluationList';
import EvaluationResultList from './pages/evaluations/EvaluationResultList';
import EvaluationResultDetail from './pages/evaluations/EvaluationResultDetail';
import AgentLogList from './pages/logs/AgentLogList';
import SessionLogList from './pages/logs/SessionLogList';
import MemoryList from './pages/memory/MemoryList';
import MemoryDetail from './pages/memory/MemoryDetail';
import MemoryDocumentDetail from './pages/memory/MemoryDocumentDetail';
import Login from './pages/login/Login';
import UserList from './pages/users/UserList';

const { Header, Sider, Content } = Layout;

const MENU_ITEMS: NonNullable<MenuProps['items']> = [
  {
    key: '/users',
    icon: <UserOutlined />,
    label: '用户管理',
  },
  {
    key: '/models',
    icon: <ApiOutlined />,
    label: '模型管理',
  },
  {
    key: '/tools',
    icon: <ToolOutlined />,
    label: '工具管理',
  },
  {
    key: '/skills',
    icon: <ThunderboltOutlined />,
    label: '技能管理',
  },
  {
    key: '/knowledge',
    icon: <BookOutlined />,
    label: '知识库管理',
  },
  {
    key: '/agents',
    icon: <RobotOutlined />,
    label: '智能体管理',
  },
  {
    key: '/sessions',
    icon: <MessageOutlined />,
    label: '会话管理',
  },
  {
    key: '/evaluations',
    icon: <CheckCircleOutlined />,
    label: '评估管理',
  },
  {
    key: '/conversations',
    icon: <HistoryOutlined />,
    label: '会话历史',
  },
  {
    key: '/memory',
    icon: <EyeOutlined />,
    label: '记忆修改',
  },
  {
    key: '/logs',
    icon: <FileTextOutlined />,
    label: '运行日志',
  },
];

/**
 * 根据当前登录用户角色过滤侧边栏菜单：用户管理菜单仅管理员可见，其余菜单所有登录用户可见。
 * @param user 当前登录用户
 * @returns 可见的菜单项列表
 */
function getVisibleMenuItems(user: User | null): MenuProps['items'] {
  if (user?.userType === USER_TYPE_ADMIN) {
    return MENU_ITEMS;
  }
  return MENU_ITEMS.filter((item) => item !== null && item.key !== '/users');
}

/**
 * 根据用户类型返回登录后落地页路径：管理员 /users，普通用户 /models。
 * @param user 当前登录用户
 * @returns 落地页路径
 */
function getLandingPath(user: User | null): string {
  return user?.userType === USER_TYPE_ADMIN ? '/users' : '/models';
}

function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);
  // 版本号状态：修改显示名/退出等自助操作后触发重渲染，重新从 localStorage 读取当前用户
  const [, refreshUser] = useState(0);
  const [displayNameModalVisible, setDisplayNameModalVisible] = useState(false);
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [displayNameForm] = Form.useForm<{ displayName: string }>();
  const [passwordForm] = Form.useForm<{ password: string }>();
  const currentUser = getCurrentUser();
  const isLoggedIn = currentUser !== null;

  // 全局 WebSocket 连接生命周期：登录态下建立连接（含页面刷新后重连），登出/未登录时关闭
  useEffect(() => {
    if (isLoggedIn) {
      webSocketClient.connect();
    } else {
      webSocketClient.close();
    }
  }, [isLoggedIn]);

  /** 修改显示名：调用自助修改接口，成功后同步 localStorage 中的当前用户并刷新 Header 显示。 */
  const handleDisplayNameOk = async (): Promise<void> => {
    let values: { displayName: string };
    try {
      values = await displayNameForm.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      const updated = await updateCurrentUser({ displayName: values.displayName });
      saveCurrentUser(updated);
      refreshUser((v) => v + 1);
      message.success('显示名修改成功');
      setDisplayNameModalVisible(false);
    } catch {
      message.error('修改显示名失败');
    } finally {
      setSubmitting(false);
    }
  };

  /** 修改密码：调用自助修改接口（不需验证旧密码），成功后关闭弹窗。 */
  const handlePasswordOk = async (): Promise<void> => {
    let values: { password: string };
    try {
      values = await passwordForm.validateFields();
    } catch {
      return;
    }
    setSubmitting(true);
    try {
      await updateCurrentUser({ password: values.password });
      message.success('密码修改成功');
      setPasswordModalVisible(false);
      passwordForm.resetFields();
    } catch {
      message.error('修改密码失败');
    } finally {
      setSubmitting(false);
    }
  };

  /** 退出登录：调用退出接口并清除本地登录状态，跳转登录页。 */
  const handleLogout = async (): Promise<void> => {
    try {
      await logout();
    } catch {
      // 后端退出失败也继续清除本地登录状态并跳转登录页
    } finally {
      navigate('/login');
    }
  };

  /** Header 用户菜单点击：按菜单 key 打开对应弹窗或执行退出。 */
  const handleUserMenuClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'editDisplayName') {
      setDisplayNameModalVisible(true);
    } else if (key === 'editPassword') {
      setPasswordModalVisible(true);
    } else if (key === 'logout') {
      void handleLogout();
    }
  };

  // 路由守卫：未登录访问任意页面（除 /login）自动跳转登录页
  if (!currentUser && location.pathname !== '/login') {
    return <Navigate to="/login" replace />;
  }

  // 登录页为独立全屏页面，不套用主界面 Layout
  if (location.pathname === '/login') {
    return <Login />;
  }

  const landingPath = getLandingPath(currentUser);
  const menuItems = getVisibleMenuItems(currentUser);
  const selectedKeys = [location.pathname === '/' ? landingPath : location.pathname];

  // Header 用户下拉菜单：普通用户含「修改显示名/修改密码/退出」，管理员仅「修改密码/退出」
  const isAdmin = currentUser?.userType === USER_TYPE_ADMIN;
  const userMenuItems: NonNullable<MenuProps['items']> = [
    ...(isAdmin ? [] : [{ key: 'editDisplayName', label: '修改显示名' }]),
    { key: 'editPassword', label: '修改密码' },
    { key: 'logout', label: '退出' },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        width={220}
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        style={{ background: '#001529' }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: 18,
            fontWeight: 'bold',
          }}
        >
          Agent 低代码平台
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              height: '100%',
            }}
          >
            <h2 style={{ margin: 0 }}>智能化 Agent 低代码平台</h2>
            <Dropdown menu={{ items: userMenuItems, onClick: handleUserMenuClick }}>
              <Space style={{ cursor: 'pointer' }}>
                <Avatar size="small" icon={<UserOutlined />} />
                <span>{currentUser?.displayName || currentUser?.loginName}</span>
              </Space>
            </Dropdown>
          </div>
        </Header>
        <Content style={{ margin: 24 }}>
          <Routes>
            <Route path="/" element={<Navigate to={landingPath} replace />} />
            <Route path="/users" element={<UserList />} />
            <Route path="/models" element={<ModelList />} />
            <Route path="/models/:id/test" element={<ModelTest />} />
            <Route path="/tools" element={<ToolList />} />
            <Route path="/agents" element={<AgentList />} />
            <Route path="/skills" element={<SkillList />} />
            <Route path="/knowledge" element={<KnowledgeBaseList />} />
            <Route path="/knowledge/:kbId/files" element={<KnowledgeFileList />} />
            <Route path="/knowledge/:kbId/files/:fileId/edit" element={<KnowledgeFileEdit />} />
            <Route path="/sessions" element={<SessionList />} />
            <Route path="/sessions/:id/chat" element={<AgentChat />} />
            <Route path="/conversations" element={<ConversationHistory />} />
            <Route path="/conversations/:sessionId" element={<ConversationHistory />} />
            <Route path="/conversations/:conversationId/detail" element={<ConversationDetail />} />
            <Route path="/evaluations" element={<AgentEvaluationList />} />
            <Route path="/evaluations/:agentEvalId/items" element={<EvaluationList />} />
            <Route path="/evaluations/items/:evaluationId/results" element={<EvaluationResultList />} />
            <Route path="/evaluations/results/:resultId" element={<EvaluationResultDetail />} />
            <Route path="/logs" element={<SessionLogList />} />
            <Route path="/logs/:sessionId" element={<AgentLogList />} />
            <Route path="/memory" element={<MemoryList />} />
            <Route path="/memory/:sessionId/:type" element={<MemoryDetail />} />
            <Route path="/memory/:sessionId/:type/:seqRange" element={<MemoryDocumentDetail />} />
          </Routes>
        </Content>
      </Layout>

      <Modal
        title="修改显示名"
        open={displayNameModalVisible}
        onOk={handleDisplayNameOk}
        onCancel={() => setDisplayNameModalVisible(false)}
        confirmLoading={submitting}
        width={420}
        destroyOnClose
      >
        <Form form={displayNameForm} layout="vertical" preserve={false}>
          <Form.Item
            name="displayName"
            label="显示名"
            rules={[{ required: true, message: '请输入显示名' }]}
          >
            <Input placeholder="请输入显示名" maxLength={50} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="修改密码"
        open={passwordModalVisible}
        onOk={handlePasswordOk}
        onCancel={() => setPasswordModalVisible(false)}
        confirmLoading={submitting}
        width={420}
        destroyOnClose
      >
        <Form form={passwordForm} layout="vertical" preserve={false}>
          <Form.Item
            name="password"
            label="新密码"
            rules={[{ required: true, message: '请输入新密码' }]}
          >
            <Input.Password placeholder="请输入新密码" maxLength={100} />
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  );
}

export default App;
