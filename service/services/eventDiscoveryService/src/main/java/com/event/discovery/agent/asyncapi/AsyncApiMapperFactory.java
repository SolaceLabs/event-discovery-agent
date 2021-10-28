package com.event.discovery.agent.asyncapi;

import com.event.discovery.agent.framework.exception.DiscoverySupportCode;
import com.event.discovery.agent.framework.exception.EventDiscoveryAgentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AsyncApiMapperFactory {
    private CommonModelToAsyncAPIMapper_2_2 asyncApi2_2Mapper;
    public AsyncApiMapperFactory(CommonModelToAsyncAPIMapper_2_2 asyncApi2_2Mapper) {
        this.asyncApi2_2Mapper = asyncApi2_2Mapper;
    }
    public CommonModelToAsyncApiMapper getAsyncApiMapper(String version) {
        switch (version) {
            case "2.2":
                return asyncApi2_2Mapper;
            default:
                log.error("Unsupported version {}", version);
                throw new EventDiscoveryAgentException(DiscoverySupportCode.DISCOVERY_ERROR_101,
                        "Unsupported version " + version);
        }
    }

}
