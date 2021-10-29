package com.event.discovery.agent.base.config;

import com.event.discovery.agent.framework.utils.IDGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
public class DiscoveryConfig {

    @Bean
    @ConditionalOnMissingBean
    public IDGenerator idGenerator() {
        return new IDGenerator();
    }

    @Bean(name = "eventDiscoveryAgentTaskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("EDA-");
        executor.setCorePoolSize(20);
        return executor;
    }
}
