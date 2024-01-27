package com.shengbinxu.excel.controller;

import com.shengbinxu.excel.entity.ExcelUser;
import com.shengbinxu.excel.entity.UserExcelHeader;
import com.shengbinxu.excel.sdk.entity.ApiResponseType;
import com.shengbinxu.excel.sdk.DownloadService;
import com.shengbinxu.excel.sdk.entity.PaginationWrapper;
import com.shengbinxu.excel.service.UserService;
import io.swagger.annotations.ApiParam;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExcelController {
    @Autowired
    DownloadService downloadService;

    @Autowired
    UserService userService;


    @GetMapping("list/{action}")
    public PaginationWrapper<ExcelUser> list(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @ApiParam("输出类型。值为json或者excel") @PathVariable("action") String action,
            HttpServletResponse response
    ) throws Exception {
        if (action.equals(ApiResponseType.JSON)) {
            PaginationWrapper<ExcelUser> pagination = userService.getList(page, pageSize);
            return pagination;
        } else if (action.equals(ApiResponseType.EXCEL)) {
            downloadService.submitTask(
                    DownloadService.RunModel.MULTI_THREAD,
                    (page2, pageSize2) -> userService.getList(page2, pageSize2),
                    100,
                    null,
                    UserExcelHeader.class,
                    "用户列表",
                    response
            );
        }
        return null;
    }
}
