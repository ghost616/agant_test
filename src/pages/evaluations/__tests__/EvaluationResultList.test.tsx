import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';

describe('EvaluationResultList 导入与路由', () => {
  it('应使用 useNavigate 和 useParams', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('useNavigate');
    expect(source).toContain('useParams');
  });
});

describe('EvaluationResultList 执行逻辑', () => {
  it('应包含停止执行按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('停止执行');
  });

  it('应包含返回按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('返回');
  });

  it('应包含执行按钮', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('执行');
  });

  it('完成后应刷新数据列表 fetchData', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('fetchData');
  });

  it('应集成 useEvaluationExecute hook', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('useEvaluationExecute');
    expect(source).toContain("from './hooks/useEvaluationExecute'");
  });

  it('handleExecute 应调用 execute(evaluationId, evaluation, fetchData)', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('handleExecute');
    expect(source).toContain('execute(evaluationId, evaluation, fetchData)');
  });
});

describe('EvaluationResultList 表格', () => {
  it('应包含 ID 列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("title: 'ID'");
  });

  it('应包含会话 ID 列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('会话ID');
  });

  it('应包含 Token 消耗列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('Token消耗');
  });

  it('应包含模型列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("title: '模型'");
  });

  it('应包含最终评分列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("title: '最终评分'");
  });

  it('最终评分配色: 60以下红色', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("score < 60");
    expect(source).toContain("color = 'red'");
  });

  it('最终评分配色: 60至80以下橙色', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("score < 80");
    expect(source).toContain("color = 'orange'");
  });

  it('最终评分配色: 80至100以下蓝色', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("score < 100");
    expect(source).toContain("color = 'blue'");
  });

  it('最终评分配色: 100绿色', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("color = 'green'");
  });

  it('应包含创建时间列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('创建时间');
  });

  it('应包含操作列', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("key: 'actions'");
  });

  it('操作列应包含查看结果按钮并导航到详情页', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('查看结果');
    expect(source).toContain('navigate(`/evaluations/results/${record.id}`)');
  });

  it('应包含执行进度显示', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('executionProgress');
  });
});

describe('EvaluationResultList 多选与批量删除', () => {
  it('应导入 batchDeleteEvaluationResults', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('batchDeleteEvaluationResults');
  });

  it('Table 应配置 rowSelection 多选', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('rowSelection');
    expect(source).toContain('selectedRowKeys');
    expect(source).toContain('onChange');
  });

  it('应使用 useState 管理 selectedRowKeys', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])");
  });

  it('批量删除按钮未勾选任何行时应 disabled', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    const buttonBlock = source.match(/danger\s+disabled=\{selectedRowKeys\.length === 0\}[\s\S]*?批量删除/s);
    expect(buttonBlock).not.toBeNull();
  });

  it('点击批量删除应触发 Modal.confirm 二次确认', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleBatchDelete[\s\S]*?}, \[selectedRowKeys, fetchData\]\);/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain('Modal.confirm');
      expect(handleBlock[0]).toContain("title: '批量删除'");
    }
  });

  it('确认后应调用 batchDeleteEvaluationResults(selectedRowKeys.map(String))', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('batchDeleteEvaluationResults(selectedRowKeys.map(String))');
  });

  it('批量删除成功后应 message.success + 清空选中 + fetchData 刷新', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleBatchDelete[\s\S]*?}, \[selectedRowKeys, fetchData\]\);/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain("message.success('批量删除成功')");
      expect(handleBlock[0]).toContain('setSelectedRowKeys([])');
      expect(handleBlock[0]).toContain('await fetchData()');
    }
  });

  it('批量删除失败时应 message.error', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("message.error('批量删除失败')");
  });
});

describe('EvaluationResultList 清空功能', () => {
  it('应导入 clearEvaluationResults', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('clearEvaluationResults');
  });

  it('清空按钮应始终可点（不设置 disabled）', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    const buttonBlock = source.match(/<Button danger onClick=\{handleClear\}>[\s\S]*?清空/s);
    expect(buttonBlock).not.toBeNull();
  });

  it('点击清空应触发 Modal.confirm 二次确认', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleClear[\s\S]*?}, \[evaluationId, fetchData\]\);/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain('Modal.confirm');
      expect(handleBlock[0]).toContain("title: '清空结果'");
    }
  });

  it('确认后应调用 clearEvaluationResults(evaluationId)', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain('clearEvaluationResults(evaluationId)');
  });

  it('清空成功后应 message.success + 清空选中 + fetchData 刷新', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    const handleBlock = source.match(/const handleClear[\s\S]*?}, \[evaluationId, fetchData\]\);/);
    expect(handleBlock).not.toBeNull();
    if (handleBlock) {
      expect(handleBlock[0]).toContain("message.success('清空成功')");
      expect(handleBlock[0]).toContain('setSelectedRowKeys([])');
      expect(handleBlock[0]).toContain('await fetchData()');
    }
  });

  it('清空失败时应 message.error', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("message.error('清空失败')");
  });
});

describe('EvaluationResultList 表格滚动 (useTableScrollY)', () => {
  it('表格 scroll 使用 useTableScrollY 实现固定表头动态高度', () => {
    const source = readFileSync(resolve(__dirname, '../EvaluationResultList.tsx'), 'utf-8');
    expect(source).toContain("import useTableScrollY from '../../hooks/useTableScrollY'");
    expect(source).toContain('scroll={{ x: 1200, y: useTableScrollY(216) }}');
  });
});
