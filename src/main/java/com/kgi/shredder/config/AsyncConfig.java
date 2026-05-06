package com.kgi.shredder.config;

import com.kgi.shredder.config.properties.KgiProperties;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "aiPipelineExecutor")
    public Executor aiPipelineExecutor(KgiProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.async().corePoolSize());
        executor.setMaxPoolSize(properties.async().maxPoolSize());
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-pipeline-");
        executor.initialize();
        return executor;
    }
}
