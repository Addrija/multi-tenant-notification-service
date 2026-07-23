package com.notification.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// The bounded worker pool that actually dispatches notifications - core/max size
// from application.yml. Bounded so a burst of due notifications can't spin up
// unlimited threads.
@Configuration
public class DispatcherConfig {

    @Bean
    public ThreadPoolTaskExecutor notificationDispatchExecutor(
            @Value("${notification.dispatcher.core-pool-size}") int corePoolSize,
            @Value("${notification.dispatcher.max-pool-size}") int maxPoolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-dispatch-");
        executor.initialize();
        return executor;
    }
}