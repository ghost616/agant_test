import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('EvaluationList 按钮文字', () => {
  it('操作列的基准会话按钮文字应为"基准会话"（已缩短）', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('基准会话');
    expect(source).not.toContain('基准会话数据产生');
  });

  it('应包含新增评估按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('新增评估');
  });

  it('应包含修改按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('修改');
  });

  it('应包含进行评估按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('进行评估');
  });

  it('应包含删除按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('删除');
  });

  it('应包含返回按钮（带 urlAgentEvalId 时）', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('返回');
  });
});

describe('EvaluationList 导入与路由', () => {
  it('应导入 useNavigate', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('useNavigate');
  });

  it('应导入 useParams', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('useParams');
  });

  it('导航到 evaluations 列表页应使用 navigate("/evaluations")', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain("navigate('/evaluations')");
  });

  it('导航到评估结果页应使用 evaluations/items/${id}/results', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('/evaluations/items/${record.id}/results');
  });

  it('导航到基准会话应使用 sessions/${id}/chat?benchmark=1', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('/sessions/${record.benchmarkSessionId}/chat?benchmark=1');
  });
});

describe('EvaluationList 基准会话条件渲染', () => {
  it('benchmarkSessionId 存在时才显示基准会话按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('record.benchmarkSessionId &&');
    expect(source).toContain('基准会话');
  });
});

describe('EvaluationList 清空结果功能', () => {
  it('应导入 clearEvaluationResults', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain('clearEvaluationResults');
  });

  it('操作列应包含清空结果按钮，且位于删除按钮之前', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    const actionsBlock = source.match(/title: '操作'[\s\S]*?},/);
    expect(actionsBlock).not.toBeNull();
    if (actionsBlock) {
      const block = actionsBlock[0];
      const clearIdx = block.indexOf('清空结果');
      const deleteIdx = block.indexOf('删除');
      expect(clearIdx).toBeGreaterThanOrEqual(0);
      expect(deleteIdx).toBeGreaterThan(clearIdx);
    }
  });

  it('清空结果按钮应为 danger 类型', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    const btnBlock = source.match(/<Button[^>]*onClick=\{\(\) => handleClearResults\(record\)\}[^>]*>[\s\S]*?清空结果/s);
    expect(btnBlock).not.toBeNull();
    if (btnBlock) {
      expect(btnBlock[0]).toContain('danger');
    }
  });

  it('清空结果按钮应始终可点（不设置 disabled）', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    const btnBlock = source.match(/<Button[^>]*onClick=\{\(\) => handleClearResults\(record\)\}[^>]*>[\s\S]*?清空结果/s);
    expect(btnBlock).not.toBeNull();
    if (btnBlock) {
      expect(btnBlock[0]).not.toContain('disabled');
    }
  });

  it('handleClearResults 应触发 Modal.confirm 二次确认', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleClearResults[\s\S]*?\n  };/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain('Modal.confirm');
      expect(handleBlock[0]).toContain("title: '清空结果'");
      expect(handleBlock[0]).toContain('content: \'确定要清空该评估下的所有评估结果吗？\'');
    }
  });

  it('确认后应调用 clearEvaluationResults(record.id)', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleClearResults[\s\S]*?\n  };/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain('clearEvaluationResults(record.id)');
    }
  });

  it('清空成功后应 message.success 并刷新列表 fetchList', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleClearResults[\s\S]*?\n  };/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain("message.success('清空成功')");
      expect(handleBlock[0]).toContain('fetchList()');
    }
  });

  it('清空失败时应 message.error', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleClearResults[\s\S]*?\n  };/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain("message.error('清空失败')");
    }
  });
});

describe('EvaluationList 表格滚动 (useTableScrollY)', () => {
  it('表格 scroll 使用 useTableScrollY 实现固定表头动态高度', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationList.tsx'), 'utf-8');
    expect(source).toContain("import useTableScrollY from '../../hooks/useTableScrollY'");
    expect(source).toContain('scroll={{ x: 1400, y: useTableScrollY(216) }}');
  });
});
