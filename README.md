# 项目目标
1、后端在做api开发时，经常既要给前端返回JSON格式的数据，又要支持Excel下载。本项目的目标是采用尽量优雅的写法，来让同一套代码支持json和excel两种返回格式。

2、后端在做excel下载时，经常会有两个痛点：
- 一次性从数据库加载所有数据，会导致OOM
- 加载的数据太多（多达数十万行），excel下载超时（服务端大概率会在nginx层、或者微服务间调用时设置一个合理的超时时间。如60s）。

针对这些痛点，我设计了一套通用的多线程、分页excel下载服务。让excel下载的代码尽可能通用、灵活、优雅。


# 使用方式

访问 `http://localhost:8080/list/json?page=2` 返回json格式数据

访问`http://localhost:8080/list/excel` 下载excel


# 设计说明
1、service中定义了“获取一页数据”的接口：
```
public interface PageDataFetcher {
    public Object get(Integer page, Integer pageSize);
}
```
2、各业务模块，实现该接口：
```
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
```

```
downloadService.submitTask(
        DownloadService.RunModel.MULTI_THREAD,
        (page2, pageSize2) -> userService.getList(page2, pageSize2), // 该lambda表达式，实现了PageDataFetcher接口
        100,
        null,
        UserExcelHeader.class,
        "用户列表",
        response
);
```

3、下载服务，支持单线程和多线程两种模式：
- 单线程模式。PageDataFetcher.get() 返回的数据格式可以是Collection（业务不需要获取总行数，只需要返回entity List就可以）、或者PaginationWrapper（需要获取总行数）

- 多线程模式。PageDataFetcher.get() 必须返回PaginationWrapper类型，告知DownloadService总共有多少行数据。然后下载服务会创建n个并行的单页下载任务，然后投递给线程池。

