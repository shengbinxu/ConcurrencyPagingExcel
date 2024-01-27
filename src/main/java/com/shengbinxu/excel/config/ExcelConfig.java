package com.shengbinxu.excel.config;

import com.shengbinxu.excel.sdk.DownloadService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "concurrency-paging-excel")
public class ExcelConfig {

    private Integer initialThreadNum = 2;
    private Integer maxThreadNum = 5;

    @Bean
    DownloadService downloadService() {
        return new DownloadService(initialThreadNum, maxThreadNum);
    }

    public Integer getInitialThreadNum() {
        return initialThreadNum;
    }

    public void setInitialThreadNum(Integer initialThreadNum) {
        this.initialThreadNum = initialThreadNum;
    }

    public Integer getMaxThreadNum() {
        return maxThreadNum;
    }

    public void setMaxThreadNum(Integer maxThreadNum) {
        this.maxThreadNum = maxThreadNum;
    }
}
