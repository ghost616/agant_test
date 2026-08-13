import { describe, it, expect, vi, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import useTableScrollY from '../useTableScrollY';

describe('useTableScrollY', () => {
  const originalInnerHeight = window.innerHeight;

  afterEach(() => {
    window.innerHeight = originalInnerHeight;
    vi.restoreAllMocks();
  });

  it('初始返回 window.innerHeight - offset', () => {
    const { result } = renderHook(() => useTableScrollY(216));
    expect(result.current).toBe(window.innerHeight - 216);
  });

  it('offset 为 0 时返回 window.innerHeight', () => {
    const { result } = renderHook(() => useTableScrollY(0));
    expect(result.current).toBe(window.innerHeight);
  });

  it('window resize 时实时更新表格高度', () => {
    const { result } = renderHook(() => useTableScrollY(216));
    window.innerHeight = 500;
    act(() => {
      window.dispatchEvent(new Event('resize'));
    });
    expect(result.current).toBe(500 - 216);
  });

  it('offset 变化时重新计算表格高度', () => {
    const { result, rerender } = renderHook(
      ({ offset }: { offset: number }) => useTableScrollY(offset),
      { initialProps: { offset: 216 } },
    );
    window.innerHeight = 1000;
    act(() => {
      rerender({ offset: 272 });
    });
    expect(result.current).toBe(1000 - 272);
  });

  it('卸载时移除 resize 监听', () => {
    const removeSpy = vi.spyOn(window, 'removeEventListener');
    const { unmount } = renderHook(() => useTableScrollY(216));
    unmount();
    expect(removeSpy).toHaveBeenCalledWith('resize', expect.any(Function));
  });

  it('innerHeight 小于 offset 时返回 0（不为负数）', () => {
    window.innerHeight = 100;
    const { result } = renderHook(() => useTableScrollY(216));
    expect(result.current).toBe(0);
  });
});
