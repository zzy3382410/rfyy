package com.current.rfyy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@EnableAsync
public class RfFpCgdMatchConfig {

    @Bean(name = "fpCgdMatchExecutor")
    public ThreadPoolTaskExecutor fpCgdMatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 任务队列容量
        executor.setQueueCapacity(200);
        // 线程名前缀
        executor.setThreadNamePrefix("Fp-Cgd-Match-Executor-");
        // 拒绝策略
        // 当队列满且线程数达到最大值时，由提交任务的线程（调用者）亲自执行该任务
        // 这样做既能保证任务不丢失，又能通过阻塞调用者来起到“天然限流”的作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 6. 优雅关机配置
        // 应用停止时，等待所有任务完成再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待 60 秒，超时强制关闭
        executor.setAwaitTerminationSeconds(60);
        // 初始化
        executor.initialize();
        return executor;
    }
}
