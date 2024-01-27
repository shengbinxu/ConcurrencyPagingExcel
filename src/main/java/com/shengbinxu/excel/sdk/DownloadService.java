package com.shengbinxu.excel.sdk;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.shengbinxu.excel.sdk.entity.PaginationWrapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * easyexcel文档：
 * https://easyexcel.opensource.alibaba.com/qa/ 只有加了@ExcelProperty注解的字段，才会下载到excel中
 * https://easyexcel.opensource.alibaba.com/docs/current/api/
 * https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read
 * 业务使用该服务时，需要先配置spring bean，并传入初始线程数、最大线程数参数
 */
public class DownloadService {

    public enum RunModel {
        SINGLE_THREAD,
        MULTI_THREAD
    }

    private PageDataFetcher pageDataFetcher;

    private EasyExcelWriter easyExcelWriter;

    private Downloader downloader;

    Logger logger = LoggerFactory.getLogger(DownloadService.class);

    private ThreadPoolExecutor fetchExecutor;

    /**
     * @param initialThreadNum 线程池初始线程数
     * @param maxThreadNum     线程池最大线程数
     */
    public DownloadService(Integer initialThreadNum, Integer maxThreadNum) {
        fetchExecutor = new ThreadPoolExecutor(initialThreadNum, maxThreadNum,
                100L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * @param runModel        单线程模式、多线程模式。
     *                        （1）“获取数据”线程组。多线程查询数据，每个线程查询一页数据，结果放入队列（实际上不是简单的队列，这里要考虑写入excel时必须按照顺序写入的问题）
     *                        （2）“写入excel”线程。会从队列中一页页获取数据，并写入excel。写入最后一页数据后，线程退出，开始给用户返回excel内容.
     * @param pageDataFetcher 业务实现该接口，定义获取一页数据的业务逻辑
     * @param pageSize        分页下载时，每页下载的行数。默认100
     * @param totalCount      要下载的数据总行数。单线程模式下，不需要业务提供totalCount。多线程模式下，先尝试看PageDataFetcher返回值中是否有totalCount，没有的话需要业务设置totalCount
     * @param headerClass     对应于easyexcel中的HeaderClass。可以是entity class，也可以在单独的类中指定header class（定义excel中需要哪些字段、字段header名称、字段顺序）
     * @param fileName        excel文件名称（不带后缀）
     * @param response        HttpServletResponse
     * @throws Exception
     */
    public void submitTask(RunModel runModel, PageDataFetcher pageDataFetcher, Integer pageSize, Integer totalCount,
                           Class headerClass, String fileName, HttpServletResponse response) throws Exception {
        this.pageDataFetcher = pageDataFetcher;
        this.easyExcelWriter = new EasyExcelWriter(headerClass, fileName, response);
        switch (runModel) {
            case MULTI_THREAD:
                downloader = new MultiThreadedDownloader(totalCount, pageSize);
                break;
            case SINGLE_THREAD:
                downloader = new SingleThreadedDownloder(totalCount, pageSize);
                break;
            default:
                throw new Exception("runModel错误");
        }
        downloader.run();
    }

    public static String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回的一页数据list
     *
     * @param pageData
     * @return
     * @throws Exception
     */
    private static Collection getPageRows(Object pageData) throws Exception {
        if (pageData instanceof PaginationWrapper) {
            return ((PaginationWrapper<?>) pageData).getData();
        } else if (pageData instanceof Collection) {
            return (Collection) pageData;
        }
        throw new Exception("不支持的返回值类型");
    }


    interface Downloader {
        public void run() throws Exception;
    }

    class SingleThreadedDownloder implements Downloader {

        private Integer totalCount;
        private Integer pageSize;

        public SingleThreadedDownloder(Integer totalCount, Integer pageSize) {
            this.totalCount = totalCount;
            this.pageSize = pageSize;
        }

        @Override
        public void run() throws Exception {
            Integer page = 1;
            while (true) {
                Object pageData = pageDataFetcher.get(page, pageSize);
                //一页返回的数据list
                Collection pageRows = getPageRows(pageData);
                Integer pageRowCount = pageRows.size();
                easyExcelWriter.write(pageRows);
                if (pageRowCount < pageSize) {
                    break;
                } else {
                    page++;
                }
            }
            easyExcelWriter.finish();
        }
    }

    class MultiThreadedDownloader implements Downloader {
        private Integer totalCount;
        private Integer pageSize;

        ConcurrentHashMap<Integer, Collection> fetcherResult = new ConcurrentHashMap();

        public MultiThreadedDownloader(Integer totalCount, Integer pageSize) {
            this.totalCount = totalCount;
            this.pageSize = pageSize;
        }

        @Override
        public void run() throws InterruptedException, UnsupportedEncodingException {
            logger.info("excel下载：线程池当前线程数：{}, 活跃线程数：{}，允许的最大线程数:{}，已完成任务数：{}, 当前任务数：{}",
                    fetchExecutor.getPoolSize(), fetchExecutor.getActiveCount(),
                    fetchExecutor.getMaximumPoolSize(), fetchExecutor.getCompletedTaskCount(),
                    fetchExecutor.getTaskCount()
            );
            autoSetTotalCount();
            List<FetcherTask> taskQueue = new ArrayList<>();
            for (Integer pageIndex = 1; pageIndex <= getTotalPage(); pageIndex++) {
                taskQueue.add(new FetcherTask(pageIndex));
            }
            // 不用阻塞等待该任务。理论上，当该任务运行完成之后，writeToExcel()方法才能执行完。
            for (Callable task : taskQueue) {
                Future<Collection> future = fetchExecutor.submit(task);
//                Collection pageData = future.get();
//                logger.info(Arrays.deepToString(pageData.toArray()));
            }
            writeToExcel();
        }

        private Integer getTotalPage() {
            return totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
        }

        private void writeToExcel() throws InterruptedException, UnsupportedEncodingException {
            logger.info("begin writeToExcel");
            // 准备写入数据的页码
            Integer pendingPage = 1;
            long begin = System.currentTimeMillis();
            while (pendingPage <= getTotalPage()) {
                if (fetcherResult.containsKey(pendingPage)) {
                    easyExcelWriter.write(fetcherResult.get(pendingPage));
                    logger.info("page {} has writed to excel", pendingPage);
                    // 处理完从map中删除数据，防止OOM
                    fetcherResult.remove(pendingPage);
                    pendingPage++;
                } else {
                    Thread.sleep(10);
                    // 超过10分钟任务还未执行完，退出
                    if (System.currentTimeMillis() - begin > 600000) {
                        logger.warn("download excel has run over 10 minutes, exit");
                        break;
                    }
                }
            }
            easyExcelWriter.finish();
        }

        public void autoSetTotalCount() {
            if (totalCount == null) {
                Object pageData = pageDataFetcher.get(1, pageSize);
                if (pageData instanceof PaginationWrapper) {
                    totalCount = ((PaginationWrapper<?>) pageData).getTotalCount();
                    logger.info("自动从PageDataFetcher获取totalCount:{}", totalCount);
                } else {
                    throw new InvalidParameterException("多线程模式下，PageDataFetcher返回值不是PaginationWrapper类型，无法自动获取totalCount。必须设置totalCount参数");
                }
            }
        }

        class FetcherTask implements Callable {
            Integer page;

            public FetcherTask(Integer page) {
                this.page = page;
            }

            @Override
            public Collection call() throws Exception {
                logger.info("begin get page {} data", page);
                Collection list = getPageRows(pageDataFetcher.get(page, pageSize));
                logger.info("get page {} data completed", page);
                fetcherResult.put(page, list);
                return list;
            }
        }
    }

    class EasyExcelWriter {
        @Nonnull Class headerClass;
        String filename;
        ExcelWriter excelWriter;
        WriteSheet writeSheet;
        HttpServletResponse response;
        AtomicBoolean isFirstWrite = new AtomicBoolean(true); // 是否是首次写入（首次调用write方法）

        public EasyExcelWriter(Class headerClass, String filename, HttpServletResponse response) throws IOException {
            this.response = response;
            this.headerClass = headerClass;
            this.filename = encodeFilename(filename);
            excelWriter = EasyExcel.write(response.getOutputStream(), headerClass)
                    .build();
            writeSheet = EasyExcel.writerSheet("Sheet0").needHead(true).build();
        }

        /**
         * runtimeException cached in global exception interceptor!
         * java.lang.IllegalArgumentException: Attempting to write a row[0] in the range [0,60072] that is already written to disk.
         * @TODO: excel数据行数大于5万时，这里会抛出异常。https://github.com/alibaba/easyexcel/issues/2291
         * @param pageRows
         * @return
         */
        public ExcelWriter write(Collection pageRows) {
            if (isFirstWrite.get()) {
                response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
                isFirstWrite.set(false);
            }
            if (pageRows.size() > 0) {
                return excelWriter.write(pageRows, writeSheet);
            }
            return null;
        }

        public void finish() {
            try {
                excelWriter.finish();
            }catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }
}
