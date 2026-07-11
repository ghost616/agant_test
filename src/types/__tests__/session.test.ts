import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('Session 类型字段', () => {
  it('Session 接口应包含 parentSessionId?: string 字段', () => {
    const source = readFileSync(resolve(__dirname, '../session.ts'), 'utf-8');
    expect(source).toContain('parentSessionId?: string');
  });

  it('Session 接口应包含 isChild?: boolean 字段', () => {
    const source = readFileSync(resolve(__dirname, '../session.ts'), 'utf-8');
    expect(source).toContain('isChild?: boolean');
  });

  it('Session 接口应定义在 export interface Session 中', () => {
    const source = readFileSync(resolve(__dirname, '../session.ts'), 'utf-8');
    const sessionMatch = source.match(/export interface Session \{[\s\S]*?^\}/m);
    expect(sessionMatch).not.toBeNull();
    if (sessionMatch) {
      expect(sessionMatch[0]).toContain('parentSessionId?: string');
      expect(sessionMatch[0]).toContain('isChild?: boolean');
    }
  });
});
