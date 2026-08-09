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
