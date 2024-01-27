package com.shengbinxu.excel.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserExcelHeader {
    @ExcelProperty(value = "用户ID", order = 1)
    private Integer id;

    @ExcelProperty(value = "用户名", order = 2)
    private String username;

    @ExcelProperty(value = "角色名称", order = 3)
    private String  roleName;
}
