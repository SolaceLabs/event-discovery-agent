package com.event.discovery.agent;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.slf4j.MDC;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@EnableScheduling
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@ComponentScan(basePackages = {"com.event.discovery", "com.rabbitmq", "com.hivemq", "io.nats"}, excludeFilters = {@ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE)
})
@EnableAsync
public class EventDiscoveryAgentApplication {
    protected static final Map<String, Object> startupStartedMarker = new HashMap();
    protected static final Map<String, Object> startupCompletedMarker = new HashMap();

    public static void main(String[] args) {
        log.info(Markers.appendEntries(startupStartedMarker), String.format("Starting event-discovery-agent --> %s", Arrays.toString(args)));

        MDC.put("pid", ManagementFactory.getRuntimeMXBean().getName());
        new SpringApplicationBuilder()
                .bannerMode(Banner.Mode.CONSOLE)
                .sources(EventDiscoveryAgentApplication.class)
                .properties(getDefault())
                .run(args);

        log.info(Markers.appendEntries(startupCompletedMarker), String.format("Started event-discovery-agent --> %s", Arrays.toString(args)));
    }

    public static Properties getDefault() {
        Properties properties = new Properties();
        properties.put("server.port", "8120");

        properties.put("spring.application.name", "event-discovery-agent");

        properties.put("event.discovery.id", "local");
        properties.put("event.discovery.jobProperties.cacheSize", 100);
        properties.put("event.discovery.jobProperties.cacheDuration", 24L);
        properties.put("event.discovery.jobProperties.cacheDurationUnit", "hours");
        properties.put("event.discovery.numConsumers", 20);
        properties.put("event.discovery.version", "0.1.0");
        properties.put("event.discovery.includeConfiguration", false);
        properties.put("event.discovery.offline", true);

        properties.put("plugin.solace.httpSempV2PageSize", 500);

        properties.put("spring.config.import", "optional:configserver:");

        return properties;
    }
}
