package com.codegym.appticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // Số thread mặc định chạy
        executor.setMaxPoolSize(20);        // Số thread tối đa khi hàng đợi đầy
        executor.setQueueCapacity(500);     // Số task tối đa trong hàng đợi chờ
        executor.setThreadNamePrefix("Async-Exec-");
        
        // CallerRunsPolicy: Nếu pool đầy và hàng đợi đầy, 
        // task sẽ được chạy trực tiếp bởi thread gọi nó (main thread),
        // giúp giảm áp lực request thay vì ném lỗi.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}
