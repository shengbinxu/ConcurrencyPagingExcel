package com.shengbinxu.excel.service;

import com.shengbinxu.excel.entity.ExcelUser;
import com.shengbinxu.excel.sdk.entity.PaginationWrapper;

public interface UserService {
    /**
     * 模拟从数据库中获取一页用户信息的效果
     *
     * @param page
     * @param pageSize
     * @return
     */
    public PaginationWrapper<ExcelUser> getList(Integer page, Integer pageSize);
}
