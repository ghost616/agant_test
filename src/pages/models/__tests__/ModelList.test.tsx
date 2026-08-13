import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { PlatformConfig } from '../../../types/model';

const mockNavigate = vi.fn();

beforeAll(() => {
  window.matchMedia = window.matchMedia || ((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
});

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockCreateModel = vi.fn().mockResolvedValue({ id: 'model-1' });

vi.mock('../../../services/model', () => ({
  listModels: vi.fn().mockResolvedValue([]),
  getPlatformConfig: vi
    .fn()
    .mockResolvedValue([
      { platformType: 'CUSTOM', defaultBaseUrl: '', modelNames: [] },
    ] satisfies PlatformConfig[]),
  createModel: (data: unknown) => mockCreateModel(data),
  updateModel: vi.fn().mockResolvedValue({ id: 'model-1' }),
  deleteModel: vi.fn().mockResolvedValue(undefined),
  updateModelStatus: vi.fn().mockResolvedValue(undefined),
}));

import ModelList from '../ModelList';
function renderComponent() {
  return render(
    <MemoryRouter>
      <ModelList />
    </MemoryRouter>,
  );
}

async function openAddModal() {
  renderComponent();
  fireEvent.click(screen.getByText('新增模型'));
  await waitFor(() => {
    expect(screen.getByText('新增模型', { selector: '.ant-modal-title' })).toBeTruthy();
  });
}

/** 判断某个表单字段对应的 Form.Item 是否隐藏（antd hidden 会加 ant-form-item-hidden 类） */
function isFormItemHidden(fieldId: string): boolean {
  const item = document.querySelector(`#${fieldId}`)?.closest('.ant-form-item');
  return !!item && item.className.includes('ant-form-item-hidden');
}

/** 通过 label 定位 antd Form.Item 下的 Select，并点击选项 */
async function selectFormField(labelText: string, optionText: string) {
  const formItem = screen.getByLabelText(labelText) as HTMLElement;
  fireEvent.mouseDown(formItem);
  const option = await screen.findByText(optionText, { selector: '.ant-select-item-option-content' });
  fireEvent.click(option);
}

async function switchPlatformToCustom() {
  await selectFormField('平台类型', '自定义');
}

describe('ModelList modelType visible 条件（CUSTOM 显示 modelType）', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockCreateModel.mockClear();
  });

  it('CUSTOM 平台下模型类型下拉框可见', async () => {
    openAddModal();
    await switchPlatformToCustom();
    await waitFor(() => {
      expect(isFormItemHidden('modelType')).toBe(false);
    });
    expect(document.querySelector('#modelType')).toBeTruthy();
    expect(document.querySelector('label[for="modelType"]')).toBeTruthy();
  });

  it('CUSTOM + EMBEDDINGS 时隐藏请求类型字段', async () => {
    openAddModal();
    await switchPlatformToCustom();
    await waitFor(() => {
      expect(isFormItemHidden('modelType')).toBe(false);
    });
    await selectFormField('模型类型', 'Embeddings');
    await waitFor(() => {
      expect(isFormItemHidden('requestType')).toBe(true);
    });
  });

  it('CUSTOM + LLM 时保留请求类型字段', async () => {
    openAddModal();
    await switchPlatformToCustom();
    await waitFor(() => {
      expect(isFormItemHidden('modelType')).toBe(false);
    });
    // 默认 modelType=LLM，请求类型应可见
    expect(isFormItemHidden('requestType')).toBe(false);
  });
});

describe('ModelList 提交时 CUSTOM 保留 modelType', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockCreateModel.mockClear();
  });

  it('CUSTOM + EMBEDDINGS 提交时 createModel 得到 modelType=EMBEDDINGS', async () => {
    openAddModal();
    await switchPlatformToCustom();
    await waitFor(() => {
      expect(isFormItemHidden('modelType')).toBe(false);
    });
    await selectFormField('模型类型', 'Embeddings');

    fireEvent.change(screen.getByLabelText('名称'), { target: { value: 'embed-model' } });
    fireEvent.change(screen.getByLabelText('Base URL'), { target: { value: 'http://localhost:8080' } });
    fireEvent.change(screen.getByLabelText('模型名称'), { target: { value: 'text-embedding-v3' } });

    fireEvent.click(screen.getByRole('button', { name: 'OK' }));
    await waitFor(() => {
      expect(mockCreateModel).toHaveBeenCalledTimes(1);
    });
    const calledData = mockCreateModel.mock.calls[0][0] as { modelType: string };
    expect(calledData.modelType).toBe('EMBEDDINGS');
  });
});

describe('ModelList 表格滚动 (useTableScrollY)', () => {
  it('表格 scroll 使用 useTableScrollY 实现固定表头动态高度', () => {
    const source = readFileSync(resolve(__dirname, '../ModelList.tsx'), 'utf-8');
    expect(source).toContain("import useTableScrollY from '../../hooks/useTableScrollY'");
    expect(source).toContain('scroll={{ x: 1360, y: useTableScrollY(216) }}');
  });
});
