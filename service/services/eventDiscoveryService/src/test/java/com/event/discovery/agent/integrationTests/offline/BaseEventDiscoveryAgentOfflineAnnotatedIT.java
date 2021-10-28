package com.event.discovery.agent.integrationTests.offline;

import com.event.discovery.agent.TestConfig;
import com.event.discovery.agent.integrationTests.BaseEventDiscoveryAgentIT;
import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.ChannelToSchemaRelationship;
import com.event.discovery.agent.types.event.common.Client;
import com.event.discovery.agent.types.event.common.ClientToChannelRelationship;
import com.event.discovery.agent.types.event.common.ClientToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.types.event.common.Schema;
import com.event.discovery.agent.types.event.common.Subscription;
import com.event.discovery.agent.framework.model.CommonModelConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestConfig.class)
@ActiveProfiles("OFFLINETEST")
public abstract class BaseEventDiscoveryAgentOfflineAnnotatedIT extends BaseEventDiscoveryAgentIT {
    @LocalServerPort
    protected int port;

    @Override
    protected int getPort() {
        return port;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getAttributeFromMap(final Map<String, Object> map, final String key) {
        if (map == null) {
            return null;
        }

        return (T) map.get(key);
    }

    protected boolean hasClientToSubscriptionRelationship(
            final List<ClientToSubscriptionRelationship> relationships,
            final Client client,
            final Subscription subscription) {
        return relationships.stream().anyMatch(rel -> rel.getClientId().equals(client.getId())
                && rel.getSubscriptionId().equals(subscription.getId()));
    }

    protected boolean hasClientToChannelRelationship(
            final List<ClientToChannelRelationship> relationships,
            final Client client,
            final Channel channel) {
        return relationships.stream().anyMatch(rel -> rel.getClientId().equals(client.getId())
                && rel.getChannelId().equals(channel.getId()));
    }

    protected boolean hasChannelToSchemaRelationship(
            final List<ChannelToSchemaRelationship> relationships,
            final Channel channel,
            final Schema schema) {
        return hasChannelToSchemaRelationship(relationships, channel, schema, null);
    }

    protected boolean hasChannelToSchemaRelationship(
            final List<ChannelToSchemaRelationship> relationships,
            final Channel channel,
            final Schema schema,
            final Schema keySchema) {
        return relationships.stream()
                .anyMatch(rel -> {
                            boolean hasChannelId = rel.getChannelId().equals(channel.getId());
                            boolean hasValueSchema = rel.getSchemaId().equals(schema.getId());
                            boolean hasKeySchema = keySchema == null || getAttributeFromMap(rel.getAdditionalAttributes(),
                                    CommonModelConstants.ChannelToSchemaRelationship.KEY_SCHEMA_ID)
                                    .equals(keySchema.getId());

                            return hasChannelId && hasValueSchema && hasKeySchema;
                        }
                );
    }

    /**
     * This returns a list of key/value schema pairs for a given channel.
     * {@link Pair#getLeft} = key schema, {@link Pair#getRight()} = value schema
     * <p>
     * This method makes a hash map of the channels each time.
     * If using large data sets, build in a version of this with a cache.
     * </p>
     *
     * @param discoveryData all the data discovered
     * @param channel       target channel
     * @return list of key/value schema pairs (keys can be null, values should not be)
     */
    protected List<Pair<Schema, Schema>> getSchemaByChannel(
            final DiscoveryData discoveryData,
            final Channel channel) {
        if (discoveryData == null
                || discoveryData.getSchemas() == null
                || discoveryData.getChannelToSchemaRelationships() == null
                || channel == null
        ) {
            return Collections.emptyList();
        }

        Map<String, Schema> schemaIdsMap = discoveryData.getSchemas().stream()
                .collect(Collectors.toMap(Schema::getId, Function.identity()));

        return discoveryData.getChannelToSchemaRelationships().stream()
                .filter(rel -> rel.getChannelId().equals(channel.getId()))
                .map(rel -> {
                    Schema valueSchema = schemaIdsMap.get(rel.getSchemaId());
                    String keySchemaId = getAttributeFromMap(rel.getAdditionalAttributes(),
                            CommonModelConstants.ChannelToSchemaRelationship.KEY_SCHEMA_ID);
                    Schema keySchema = schemaIdsMap.get(keySchemaId);
                    return Pair.of(keySchema, valueSchema);
                }).collect(Collectors.toList());
    }
}