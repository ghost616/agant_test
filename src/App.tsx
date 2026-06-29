import { useState } from 'react';
import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import type { MenuProps } from 'antd';
import {
  ApiOutlined,
  MessageOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import ModelList from './pages/models/ModelList';
import ModelTest from './pages/models/ModelTest';
import ToolList from './pages/tools/ToolList';
import AgentList from './pages/agents/AgentList';
import SessionList from './pages/sessions/SessionList';
import AgentChat from './pages/sessions/AgentChat';
import SkillList from './pages/skills/SkillList';

const { Header, Sider, Content } = Layout;

const MENU_ITEMS: MenuProps['items'] = [
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
    key: '/agents',
    icon: <RobotOutlined />,
    label: '智能体管理',
  },
  {
    key: '/sessions',
    icon: <MessageOutlined />,
    label: '会话管理',
  },
];

function App() {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const selectedKeys = [location.pathname === '/' ? '/models' : location.pathname];

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
          Agent 调试平台
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={MENU_ITEMS}
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
          <h2 style={{ margin: 0 }}>智能化 Agent 调试平台</h2>
        </Header>
        <Content style={{ margin: 24 }}>
          <Routes>
            <Route path="/" element={<ModelList />} />
            <Route path="/models" element={<ModelList />} />
            <Route path="/models/:id/test" element={<ModelTest />} />
            <Route path="/tools" element={<ToolList />} />
            <Route path="/agents" element={<AgentList />} />
            <Route path="/skills" element={<SkillList />} />
            <Route path="/sessions" element={<SessionList />} />
            <Route path="/sessions/:id/chat" element={<AgentChat />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}

export default App;
