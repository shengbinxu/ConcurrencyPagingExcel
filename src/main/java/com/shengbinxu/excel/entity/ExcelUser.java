package com.shengbinxu.excel.entity;


/**
 * 往往需要对表中的数据做一些加工，然后返回给用户。
 * ExcelUser类在User类基础上，增加roleName属性
 */
public class ExcelUser extends User {
    public ExcelUser() {
        super();
    }

    public ExcelUser(Integer id, String username, Integer roleId, Integer sex, String photoUrl) {
        super(id, username, roleId, sex, photoUrl);
    }

    private String roleName;

    public String getRoleName() {
        return getRoleId() == 1 ? "管理员" : "普通用户";
    }
}
