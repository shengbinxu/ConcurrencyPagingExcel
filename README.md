# 项目目标
1、后端在做api开发时，经常既要给前端返回JSON格式的数据，又要支持Excel下载。本项目目标，采用尽量优雅的写法，来让同一套代码支持json和excel两种返回格式。

2、后端在做excel下载时，经常会有两个痛点：
- 一次性从数据库加载所有数据，会导致OOM
- 加载的数据太多（多达数十万行），excel下载超时（服务端大概率会在nginx层、或者微服务间调用时设置一个合理的超时时间。如60s）。

针对这些痛点，我设计了一套通用的多线程、分页excel下载服务。让excel下载的代码尽可能通用、灵活、优雅。


# 使用方式

访问 `http://localhost:8080/list/json?page=2` 返回json格式数据

访问`http://localhost:8080/list/excel` 下载excel
