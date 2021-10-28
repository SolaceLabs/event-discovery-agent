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

//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(name = "event.discovery.offline", havingValue = "true")
//    public VMRConnector getVmrConnector() {
//        return new StandaloneVmrConnector();
//    }

    // Bypass authentication for stand-alone
//    @Primary
//    @ConditionalOnProperty(name = "event.discovery.offline", havingValue = "true")
//    @Bean(name = "shiroFilter")
//    public ShiroFilterFactoryBean shiroFilter(@Qualifier("securityManager") SecurityManager securityManager) {
//        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
//        bean.setSecurityManager(securityManager);
//
//        Map<String, Filter> filters = new HashMap<>();
//        filters.put("perms", internalStandaloneAuthenticationTokenFilter());
//        filters.put("anon", new AnonymousFilter());
//        bean.setFilters(filters);
//
//        LinkedHashMap<String, String> chains = new LinkedHashMap<>();
//        chains.put("/api/**", "perms");
//
//        bean.setFilterChainDefinitionMap(chains);
//        return bean;
//    }

//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(name = "event.discovery.offline", havingValue = "true")
//    @Bean
//    public InternalStandaloneAuthenticationTokenFilter internalStandaloneAuthenticationTokenFilter() {
//        return new InternalStandaloneAuthenticationTokenFilter();
//    }

//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(name = "event.discovery.offline", havingValue = "true")
//    @Bean
//    public P2PReceiver standAloneP2PReceiver() {
//        VMRConnector dummyConnector = new StandaloneVmrConnector();
//        P2PReceiver p2PReceiver = null;
//        try {
//            p2PReceiver = new P2PReceiver(dummyConnector);
//        } catch (JCSMPException e) {
//            e.printStackTrace();
//        }
//        return p2PReceiver;
//    }

//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(name = "event.discovery.offline", havingValue = "true")
//    @Bean
//    public HeartbeatProperties getHeartbeatProperties() {
//        HeartbeatProperties dummyHeartbeat = new HeartbeatProperties();
//        dummyHeartbeat.setOriginId("dummy");
//        return dummyHeartbeat;
//    }


    @Bean(name = "eventDiscoveryAgentTaskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("EDA-");
        executor.setCorePoolSize(20);
        return executor;
    }

//    @Bean
//    public CurrentTraceContext.ScopeDecorator legacyMDCKeys() {
//        return MDCScopeDecorator.newBuilder()
//                .clear()
//                .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageFields.TRACE_ID)
//                        .name("X-B3-TraceId").build())
//                .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageFields.PARENT_ID)
//                        .name("X-B3-ParentSpanId").build())
//                .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageFields.SPAN_ID)
//                        .name("X-B3-SpanId").build())
//                .add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageFields.SAMPLED)
//                        .name("X-Span-Export").build())
//                .build();
//    }

}
