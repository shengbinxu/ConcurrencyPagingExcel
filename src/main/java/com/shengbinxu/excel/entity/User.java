package com.shengbinxu.excel.entity;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@ExcelIgnoreUnannotated
@NoArgsConstructor
/**
 * 在ORM中，User类代表数据库中的User表。
 */
public class User {
    private Integer id;

    private String username;

    private Integer roleId;

    private Integer sex;

    private String photoUrl;
}
