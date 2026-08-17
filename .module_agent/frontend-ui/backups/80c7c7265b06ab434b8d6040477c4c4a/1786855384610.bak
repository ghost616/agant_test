import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

const appPath = resolve(__dirname, '../App.tsx');

describe('App 路由注册 (功能点7)', () => {
  it('应导入 KnowledgeFileEdit 组件', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain("import KnowledgeFileEdit from './pages/knowledge/KnowledgeFileEdit'");
  });

  it('应注册路由 /knowledge/:kbId/files/:fileId/edit 指向 KnowledgeFileEdit', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain(
      'path="/knowledge/:kbId/files/:fileId/edit" element={<KnowledgeFileEdit />}',
    );
  });
});

describe('App 路由注册 (运行日志)', () => {
  it('应导入 AgentLogList 组件', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain("import AgentLogList from './pages/logs/AgentLogList'");
  });

  it('应注册路由 /logs 指向 AgentLogList', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain('path="/logs" element={<AgentLogList />}');
  });

  it('侧边栏应包含运行日志菜单项（FileTextOutlined）', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain('FileTextOutlined');
    expect(source).toContain("key: '/logs'");
    expect(source).toContain("label: '运行日志'");
  });
});

describe('App 路由注册 (记忆修改)', () => {
  it('应导入 MemoryList 与 MemoryDetail 组件', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain("import MemoryList from './pages/memory/MemoryList'");
    expect(source).toContain("import MemoryDetail from './pages/memory/MemoryDetail'");
  });

  it('应注册路由 /memory 指向 MemoryList', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain('path="/memory" element={<MemoryList />}');
  });

  it('应注册路由 /memory/:sessionId/:type 指向 MemoryDetail', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain('path="/memory/:sessionId/:type" element={<MemoryDetail />}');
  });

  it('应导入 MemoryDocumentDetail 组件并注册详情路由', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain(
      "import MemoryDocumentDetail from './pages/memory/MemoryDocumentDetail'",
    );
    expect(source).toContain(
      'path="/memory/:sessionId/:type/:seqRange" element={<MemoryDocumentDetail />}',
    );
  });

  it('侧边栏应包含记忆修改菜单项（EyeOutlined，key: /memory，label: 记忆修改）', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain('EyeOutlined');
    expect(source).toContain("key: '/memory'");
    expect(source).toContain("label: '记忆修改'");
  });
});

describe('App 登录守卫与菜单权限', () => {
  it('应导入 getCurrentUser、USER_TYPE_ADMIN 与 Navigate', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain("import { getCurrentUser, logout, saveCurrentUser } from './services/auth'");
    expect(source).toContain("import { USER_TYPE_ADMIN } from './types/user'");
    expect(source).toContain('Navigate');
  });

  it('路由守卫：未登录访问任意页面（除 /login）自动跳转登录页', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain(
      "if (!currentUser && location.pathname !== '/login') {",
    );
    expect(source).toContain('return <Navigate to="/login" replace />;');
  });

  it('根路由 / 应按角色重定向落地页（管理员 /users、普通用户 /models）', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain(
      "return user?.userType === USER_TYPE_ADMIN ? '/users' : '/models';",
    );
    expect(source).toContain(
      '<Route path="/" element={<Navigate to={landingPath} replace />} />',
    );
    expect(source).toContain("const landingPath = getLandingPath(currentUser);");
  });

  it('侧边栏菜单按角色过滤：用户管理菜单仅管理员可见', () => {
    const source = readFileSync(appPath, 'utf-8');
    expect(source).toContain(
      "MENU_ITEMS.filter((item) => item !== null && item.key !== '/users')",
    );
    expect(source).toContain('items={menuItems}');
    expect(source).toContain('const menuItems = getVisibleMenuItems(currentUser);');
  });
});
