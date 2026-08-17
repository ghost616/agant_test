import { useState } from 'react';
import { Navigate, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import type { MenuProps } from 'antd';
import { getCurrentUser } from './services/auth';
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
  const currentUser = getCurrentUser();

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
          <h2 style={{ margin: 0 }}>智能化 Agent 低代码平台</h2>
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
            <Route path="/logs" element={<AgentLogList />} />
            <Route path="/memory" element={<MemoryList />} />
            <Route path="/memory/:sessionId/:type" element={<MemoryDetail />} />
            <Route path="/memory/:sessionId/:type/:seqRange" element={<MemoryDocumentDetail />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}

export default App;
