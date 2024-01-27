package com.shengbinxu.excel.sdk.entity;

import java.io.Serializable;
import java.util.List;

public class PaginationWrapper<T> implements Serializable {

    private Integer page;

    private Integer pageSize;

    private Integer totalCount;

    private List<T> data;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
