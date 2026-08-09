import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { LogLevel, LogType } from '../log';

describe('LogType 枚举', () => {
  it('不应包含 CALL_SOURCE', () => {
    const source = readFileSync(resolve(__dirname, '../log.ts'), 'utf-8');
    expect(source).not.toContain('CALL_SOURCE');
  });

  it('所有条目的 code 与 label 均非空', () => {
    Object.values(LogType).forEach((item) => {
      expect(item.code).toBeTruthy();
      expect(item.label).toBeTruthy();
    });
  });
});

describe('LogLevel 枚举', () => {
  it('仅保留 INFO 与 ERROR，不含 WARN', () => {
    const codes = Object.values(LogLevel).map((item) => item.code);
    expect(codes).toEqual(['INFO', 'ERROR']);
  });

  it('INFO/ERROR 的 code 与 label 正确', () => {
    expect(LogLevel.INFO).toEqual({ code: 'INFO', label: '信息' });
    expect(LogLevel.ERROR).toEqual({ code: 'ERROR', label: '错误' });
  });

  it('源文件中不存在 WARN 定义', () => {
    const source = readFileSync(resolve(__dirname, '../log.ts'), 'utf-8');
    expect(source).not.toContain('WARN');
  });
});
