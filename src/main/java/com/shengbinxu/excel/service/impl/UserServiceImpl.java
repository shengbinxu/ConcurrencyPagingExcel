package com.shengbinxu.excel.service.impl;

import com.shengbinxu.excel.entity.ExcelUser;
import com.shengbinxu.excel.entity.User;
import com.shengbinxu.excel.sdk.entity.PaginationWrapper;
import com.shengbinxu.excel.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService, InitializingBean {

    private User[] allUsers = new User[100000];

    @Override
    public void afterPropertiesSet() {
        Integer uid = 1;
        for (int i = 0; i < allUsers.length; i++) {
            User user = new User(uid, "name-" + uid, (i % 2) + 1, 1, "http://xxxx");
            allUsers[i] = user;
            uid = uid + 1;
        }
    }

    /**
     * 模拟从数据库中获取一页用户信息的效果
     *
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PaginationWrapper<ExcelUser> getList(Integer page, Integer pageSize) {
        List<ExcelUser> pageUsers = new ArrayList<>();
        for (int i = ((page - 1) * pageSize); i < page * pageSize; i++) {
            ExcelUser excelUser = new ExcelUser();
            BeanUtils.copyProperties(allUsers[i], excelUser);
            pageUsers.add(excelUser);
        }
        PaginationWrapper<ExcelUser> paginationWrapper = new PaginationWrapper<>();
        paginationWrapper.setData(pageUsers);
        paginationWrapper.setPage(page);
        paginationWrapper.setPageSize(pageSize);
        paginationWrapper.setTotalCount(allUsers.length);
        return paginationWrapper;
    }


}
