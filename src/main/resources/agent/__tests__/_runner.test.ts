import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('_runner.ts AgentExecutionContext stopped field', () => {
  it('接口应包含 stopped?: boolean 字段定义', () => {
    const source = readFileSync(resolve(__dirname, '../_runner.ts'), 'utf-8');
    expect(source).toContain('stopped?: boolean');
  });

  it('AgentExecutionContext 接口存在且包含 stopped', () => {
    const source = readFileSync(resolve(__dirname, '../_runner.ts'), 'utf-8');
    const interfaceMatch = source.match(/export interface AgentExecutionContext \{[\s\S]*?\}/);
    expect(interfaceMatch).not.toBeNull();
    expect(interfaceMatch![0]).toContain('stopped');
  });

  it('解析包含 stopped:true 的 JSON 后可访问 stopped 字段', () => {
    const json = JSON.parse(`{
      "sessionId": "s1",
      "agentId": "a1",
      "systemPrompt": "hello",
      "modelId": "m1",
      "recentMessageCount": 5,
      "history": [],
      "tools": [],
      "skills": [],
      "sessionVariables": {},
      "conversationVariables": {},
      "stopped": true
    }`);
    expect(json.stopped).toBe(true);
  });

  it('解析不包含 stopped 的 JSON 后 stopped 为 undefined', () => {
    const json = JSON.parse(`{
      "sessionId": "s1",
      "agentId": "a1",
      "systemPrompt": "hello"
    }`);
    expect(json.stopped).toBeUndefined();
  });

  it('解析包含 stopped:false 的 JSON 后可访问 stopped 字段', () => {
    const json = JSON.parse(`{
      "sessionId": "s1",
      "stopped": false
    }`);
    expect(json.stopped).toBe(false);
  });
});
