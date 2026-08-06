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
