package com.event.discovery.agent.plugins.broker;

import com.event.discovery.agent.plugins.PluginConfigurationMap;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BrokerIdentityDeserializer extends JsonDeserializer<BrokerIdentity> {

    private final Map<String, Class<? extends BrokerIdentity>> brokerTypeToIdentityClassMap = new HashMap();

    public BrokerIdentityDeserializer(PluginConfigurationMap pluginConfigurationMap) {
        super();
        pluginConfigurationMap.getPluginConfigurationMap()
                .forEach((k, v) -> brokerTypeToIdentityClassMap.put(k, v.getBrokerIdentityClass()));
    }

    @Override
    public BrokerIdentity deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);

        if (root.has("brokerType")) {
            String brokerType = root.get("brokerType").asText();

            return mapper.readValue(root.toString(), brokerTypeToIdentityClassMap.get(brokerType));
        }
        return mapper.readValue(root.toString(), BasicBrokerIdentity.class);
    }
}
