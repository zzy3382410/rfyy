package com.current.rfyy.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * @Author: zzy
 * @Date: 2026/1/9 9:16
 * @Description: TODO 批处理工具类
 **/
@Slf4j
public class BatchProcessor<T> {

    private final BatchConfig config;
    private final ThreadPoolTaskExecutor executor;

    public BatchProcessor(BatchConfig config, ThreadPoolTaskExecutor executor) {
        this.config = config;
        this.executor = executor;
    }

    public void processInBatches(
            List<T> items,
            Consumer<List<T>> batchConsumer,
            Consumer<Throwable> errorConsumer
    ) {

        List<List<T>> batches = split(items, config.getBatchSize());

        Semaphore semaphore = new Semaphore(config.getThreadCount());
        CountDownLatch latch = new CountDownLatch(batches.size());

        for (List<T> batch : batches) {
            executor.submit(() -> {
                try {
                    semaphore.acquire(); // 限流

                    runWithRetry(() -> batchConsumer.accept(batch));

                } catch (Throwable e) {
                    errorConsumer.accept(e);
                } finally {
                    semaphore.release();
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(); // 等待所有任务完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 定义线程池不用shutdown
        // executor.shutdown();
    }

    private void runWithRetry(Runnable runnable) {
        int retry = 0;
        while (true) {
            try {
                runnable.run();
                return;
            } catch (Exception ex) {
                retry++;
                if (retry > config.getMaxRetry()) {
                    throw ex;
                }
                log.warn("batch failed, retry " + retry, ex);
            }
        }
    }

    // 将 list 分成 n 批
    private List<List<T>> split(List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return result;
    }
}
