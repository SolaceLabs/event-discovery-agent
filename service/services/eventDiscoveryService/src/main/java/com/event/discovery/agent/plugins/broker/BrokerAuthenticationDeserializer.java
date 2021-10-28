package com.event.discovery.agent.plugins.broker;

import com.event.discovery.agent.plugins.PluginConfigurationMap;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BrokerAuthenticationDeserializer extends JsonDeserializer<BrokerAuthentication> {

    private final Map<String, Class<? extends BrokerAuthentication>> brokerTypeToAuthenticationClassMap = new HashMap();

    public BrokerAuthenticationDeserializer(PluginConfigurationMap pluginConfigurationMap) {
        super();
        pluginConfigurationMap.getPluginConfigurationMap()
                .forEach((k, v) -> brokerTypeToAuthenticationClassMap.put(k, v.getBrokerAuthenticationClass()));
    }

    @Override
    public BrokerAuthentication deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);

        if (root.has("brokerType")) {
            String brokerType = root.get("brokerType").asText();

            return mapper.readValue(root.toString(), brokerTypeToAuthenticationClassMap.get(brokerType));
        }
        return mapper.readValue(root.toString(), NoAuthBrokerAuthentication.class);
    }
}
