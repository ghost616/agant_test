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
});
