import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('SessionList 表格列', () => {
  it('应包含 Token 消耗 列标题', () => {
    const source = readFileSync(resolve(__dirname, '../SessionList.tsx'), 'utf-8');
    expect(source).toContain("title: 'Token 消耗'");
  });

  it('Token 消耗列应使用 totalTokenUsed 作为 dataIndex', () => {
    const source = readFileSync(resolve(__dirname, '../SessionList.tsx'), 'utf-8');
    expect(source).toContain("dataIndex: 'totalTokenUsed'");
  });

  it('Token 消耗列应使用 toLocaleString() 格式化', () => {
    const source = readFileSync(resolve(__dirname, '../SessionList.tsx'), 'utf-8');
    expect(source).toContain('Number(value).toLocaleString()');
  });

  it('Token 消耗列空值应显示为 -', () => {
    const source = readFileSync(resolve(__dirname, '../SessionList.tsx'), 'utf-8');
    const columnBlock = source.match(/title: 'Token 消耗'[\s\S]*?},/);
    expect(columnBlock).not.toBeNull();
    if (columnBlock) {
      expect(columnBlock[0]).toContain("value != null ? Number(value).toLocaleString() : '-'");
    }
  });

  it('Token 消耗列宽度应为 120', () => {
    const source = readFileSync(resolve(__dirname, '../SessionList.tsx'), 'utf-8');
    const columnBlock = source.match(/title: 'Token 消耗'[\s\S]*?},/);
    expect(columnBlock).not.toBeNull();
    if (columnBlock) {
      expect(columnBlock[0]).toContain('width: 120');
    }
  });
});

describe('SessionList 表格滚动 (useTableScrollY)', () => {
  it('表格 scroll 使用 useTableScrollY 实现固定表头动态高度', () => {
    const source = readFileSync(resolve(__dirname, '../SessionList.tsx'), 'utf-8');
    expect(source).toContain("import useTableScrollY from '../../hooks/useTableScrollY'");
    expect(source).toContain('scroll={{ x: 960, y: useTableScrollY(216) }}');
  });
});
