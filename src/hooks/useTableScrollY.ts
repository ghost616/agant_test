import { useEffect, useState } from 'react';

function getScrollY(offset: number): number {
  return Math.max(window.innerHeight - offset, 0);
}

/**
 * 根据 window.innerHeight 减去固定偏移量动态计算表格可滚动高度，
 * 监听 window resize 实时更新，实现固定表头 + 底部可见横向滚动条。
 */
function useTableScrollY(offset = 0): number {
  const [scrollY, setScrollY] = useState<number>(() => getScrollY(offset));

  useEffect(() => {
    setScrollY(getScrollY(offset));
    const handleResize = (): void => {
      setScrollY(getScrollY(offset));
    };
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [offset]);

  return scrollY;
}

export default useTableScrollY;
