package com.event.discovery.agent.asyncapi;

import com.event.discovery.agent.apps.model.EventDiscoveryOperationDiscoveryResultBO;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class AsyncApiManager {
    private final AsyncApiMapperFactory mapperFactory;
    public AsyncApiManager(AsyncApiMapperFactory mapperFactory) {
        this.mapperFactory = mapperFactory;
    }

    // TODO: Defer to plugins if they provide their own mapper
    public JSONObject getAsyncApi(String version, EventDiscoveryOperationDiscoveryResultBO scan) {
        CommonModelToAsyncApiMapper mapper = mapperFactory.getAsyncApiMapper(version);

        if (mapper != null) {
            return mapper.map(scan);
        }
        return new JSONObject();
    }
}