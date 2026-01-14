package com.current.rfyy.batch;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: zzy
 * @Date: 2026/1/9 9:17
 * @Description: TODO
 **/
@Data
@Builder
public class BatchConfig {
    private int batchSize;   // 每批处理多少企业
    private int threadCount; // 批处理线程数
    // private int maxInSize;   // 单次 IN 查询最大值
    private int maxRetry = 0;    // 批失败重试
}

