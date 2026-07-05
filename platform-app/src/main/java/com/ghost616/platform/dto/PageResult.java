package com.ghost616.platform.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

/**
 * 分页结果响应体。
 *
 * @param <T> 列表元素类型
 */
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 从 MyBatis-Plus IPage 构建分页结果。
     *
     * @param iPage MyBatis-Plus 分页对象
     * @param list  转换后的列表数据
     * @param <T>   列表元素类型
     * @return PageResult
     */
    public static <T> PageResult<T> of(IPage<?> iPage, List<T> list) {
        return new PageResult<>(list, iPage.getTotal(), (int) iPage.getCurrent(), (int) iPage.getSize());
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
