import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('Agent 类型定义', () => {
  it('应定义 SessionAuthType 联合类型', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain("export type SessionAuthType = 'ALL' | 'PARENT' | 'CHILD'");
  });

  it('AgentConfig 接口中 tools 包含 toolId 和 sessionAuth', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('tools: { toolId: string; sessionAuth: SessionAuthType }[]');
  });

  it('AgentConfig 接口中 skills 包含 skillId 和 sessionAuth', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('skills: { skillId: string; sessionAuth: SessionAuthType }[]');
  });

  it('AgentFormData 接口中 tools 包含 toolId 和 sessionAuth', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('tools?: { toolId: string; sessionAuth: SessionAuthType }[]');
  });

  it('AgentFormData 接口中 skills 包含 skillId 和 sessionAuth', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('skills?: { skillId: string; sessionAuth: SessionAuthType }[]');
  });

  it('AgentListParams 接口应包含 name 和 status 字段', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('name?: string');
    expect(source).toContain('status?: CommonStatus');
  });

  it('应定义 KnowledgeBaseItem 接口（id + name，对齐后端 KnowledgeBaseDTO）', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('interface KnowledgeBaseItem');
    expect(source).toContain('id: string');
    expect(source).toContain('name: string');
  });

  it('AgentConfig 接口应包含 knowledgeBases?: KnowledgeBaseItem[]（用于回显）', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('knowledgeBases?: KnowledgeBaseItem[]');
    const configBlock = source.match(/interface AgentConfig[\s\S]*?\n\}/);
    expect(configBlock).not.toBeNull();
    if (configBlock) {
      expect(configBlock[0]).toContain('knowledgeBases?: KnowledgeBaseItem[]');
      expect(configBlock[0]).not.toContain('knowledgeBaseIds?: string[]');
    }
  });

  it('AgentFormData 接口应包含 knowledgeBaseIds?: string[]（用于提交）', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain('knowledgeBaseIds?: string[]');
    const formBlock = source.match(/interface AgentFormData[\s\S]*?\n\}/);
    expect(formBlock).not.toBeNull();
    if (formBlock) {
      expect(formBlock[0]).toContain('knowledgeBaseIds?: string[]');
      expect(formBlock[0]).not.toContain('knowledgeBases?: KnowledgeBaseItem[]');
    }
  });

  it('AgentConfig 接口应包含 memoryEnabled?: boolean 字段', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    const configBlock = source.match(/interface AgentConfig[\s\S]*?\n\}/);
    expect(configBlock).not.toBeNull();
    if (configBlock) {
      expect(configBlock[0]).toContain('memoryEnabled?: boolean');
    }
  });

  it('AgentConfig 接口应包含 memoryGroupCount?: number 字段', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    const configBlock = source.match(/interface AgentConfig[\s\S]*?\n\}/);
    expect(configBlock).not.toBeNull();
    if (configBlock) {
      expect(configBlock[0]).toContain('memoryGroupCount?: number');
    }
  });

  it('AgentFormData 接口应包含 memoryEnabled?: boolean 字段', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    const formBlock = source.match(/interface AgentFormData[\s\S]*?\n\}/);
    expect(formBlock).not.toBeNull();
    if (formBlock) {
      expect(formBlock[0]).toContain('memoryEnabled?: boolean');
    }
  });

  it('AgentFormData 接口应包含 memoryGroupCount?: number 字段', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    const formBlock = source.match(/interface AgentFormData[\s\S]*?\n\}/);
    expect(formBlock).not.toBeNull();
    if (formBlock) {
      expect(formBlock[0]).toContain('memoryGroupCount?: number');
    }
  });

  it('应定义 SubSessionOpenMode 联合类型', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    expect(source).toContain("export type SubSessionOpenMode = 'WEBSOCKET' | 'TOOL_CALL'");
  });

  it('AgentConfig 接口应包含 subSessionOpenMode?: SubSessionOpenMode 字段', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    const configBlock = source.match(/interface AgentConfig[\s\S]*?\n\}/);
    expect(configBlock).not.toBeNull();
    if (configBlock) {
      expect(configBlock[0]).toContain('subSessionOpenMode?: SubSessionOpenMode');
    }
  });

  it('AgentFormData 接口应包含 subSessionOpenMode?: SubSessionOpenMode 字段', () => {
    const source = readFileSync(resolve(__dirname, '../agent.ts'), 'utf-8');
    const formBlock = source.match(/interface AgentFormData[\s\S]*?\n\}/);
    expect(formBlock).not.toBeNull();
    if (formBlock) {
      expect(formBlock[0]).toContain('subSessionOpenMode?: SubSessionOpenMode');
    }
  });
});
