package com.event.discovery.agent.solace.mapper;

import com.event.discovery.agent.solace.model.Channel;
import com.event.discovery.agent.solace.model.broker.SolaceBrokerIdentity;
import com.event.discovery.agent.types.event.common.Broker;
import com.event.discovery.agent.types.event.common.ChannelToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.ClientToChannelRelationship;
import com.event.discovery.agent.types.event.common.ClientToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.framework.utils.IDGenerator;
import com.event.discovery.agent.solace.model.Client;
import com.event.discovery.agent.solace.model.Endpoint;
import com.event.discovery.agent.solace.model.EndpointImpl;
import com.event.discovery.agent.solace.model.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SolaceToCommonModelMapper {
    private final IDGenerator idGenerator;

    public SolaceToCommonModelMapper(IDGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public DiscoveryData map(SolaceBrokerIdentity brokerIdentity, Collection<Channel> channelList,
                             Collection<Client> clientList, Collection<Subscription> subscriptions) {

        List<ChannelToSubscriptionRelationship> channelToSubscriptionRelationships = new ArrayList<>();
        List<com.event.discovery.agent.types.event.common.Client> commonClientList = new ArrayList<>();
        List<ClientToSubscriptionRelationship> clientToSubscriptionRelationships = new ArrayList<>();
        List<ClientToChannelRelationship> clientToChannelRelationships = new ArrayList<>();

        DiscoveryData discoveryData = new DiscoveryData();
        discoveryData.setBroker(map(brokerIdentity));

        // Create a map with subscription string to subscription object
        // We'll use this later when were building relationships
        Map<String, com.event.discovery.agent.types.event.common.Subscription> subscriptionMap = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            subscriptionMap.put(subscription.getRawSubscription(), map(subscription));
        }
        discoveryData.setSubscriptions(new ArrayList<>(subscriptionMap.values()));

        // Create a map with channel name to channel object
        // We'll use this later when were building relationships
        Map<String, com.event.discovery.agent.types.event.common.Channel> channelMap = new HashMap<>();
        createChannelsAndChannelRelationships(channelList, subscriptionMap, channelToSubscriptionRelationships, channelMap);

        createClientsAndClientRelationships(clientList, subscriptionMap, channelMap, commonClientList,
                clientToSubscriptionRelationships, clientToChannelRelationships);

        // Set the discovery data fields
        discoveryData.setClients(commonClientList);
        discoveryData.setChannels(new ArrayList<>(channelMap.values()));
        discoveryData.setSubscriptions(new ArrayList<>(subscriptionMap.values()));
        discoveryData.setClientToChannelRelationships(clientToChannelRelationships);
        discoveryData.setClientToSubscriptionRelationships(clientToSubscriptionRelationships);
        discoveryData.setChannelToSubscriptionRelationships(channelToSubscriptionRelationships);
        return discoveryData;
    }

    private void createChannelsAndChannelRelationships(Collection<Channel> channelList,
                                                       Map<String, com.event.discovery.agent.types.event.common.Subscription> subscriptionMap,
                                                       List<ChannelToSubscriptionRelationship> channelToSubscriptionRelationships,
                                                       Map<String, com.event.discovery.agent.types.event.common.Channel> channelMap) {
        for (Channel channel : channelList) {
            com.event.discovery.agent.types.event.common.Channel commonChannel = map(channel);
            channelMap.put(getChannelUniqueName(channel), commonChannel);

            // While we're here, lets create all of the channel to subscription relationships
            if (channel instanceof Endpoint) {
                for (Subscription sub : ((Endpoint) channel).getSubscriptions()) {
                    com.event.discovery.agent.types.event.common.Subscription commonSub =
                            subscriptionMap.get(sub.getRawSubscription());
                    if (commonSub != null) {
                        ChannelToSubscriptionRelationship channelToSubscriptionRelationship = new ChannelToSubscriptionRelationship();
                        channelToSubscriptionRelationship.setChannelId(commonChannel.getId());
                        channelToSubscriptionRelationship.setSubscriptionId(commonSub.getId());
                        channelToSubscriptionRelationships.add(channelToSubscriptionRelationship);
                    }
                }
            }
        }
    }

    private void createClientsAndClientRelationships(Collection<Client> clientList, Map<String,
            com.event.discovery.agent.types.event.common.Subscription> subscriptionMap,
                                                     Map<String, com.event.discovery.agent.types.event.common.Channel> channelMap,
                                                     List<com.event.discovery.agent.types.event.common.Client> commonClientList,
                                                     List<ClientToSubscriptionRelationship> clientToSubscriptionRelationships,
                                                     List<ClientToChannelRelationship> clientToChannelRelationships) {
        for (Client client : clientList) {
            com.event.discovery.agent.types.event.common.Client commonClient = map(client);
            commonClientList.add(commonClient);

            // While we're here, lets create all of the client to subscription relationships
            for (Subscription sub : client.getRxSubscriptions()) {
                // Get the subscription id
                com.event.discovery.agent.types.event.common.Subscription commonSub =
                        subscriptionMap.get(sub.getRawSubscription());
                if (commonSub != null) {
                    ClientToSubscriptionRelationship clientToSub = new ClientToSubscriptionRelationship();
                    clientToSub.setClientId(commonClient.getId());
                    clientToSub.setSubscriptionId(commonSub.getId());
                    clientToSubscriptionRelationships.add(clientToSub);
                }
            }

            // Also while we're here, lets create all of the client to channel relationships
            for (Endpoint endpoint : client.getRxEndpoints()) {
                // the channel id
                com.event.discovery.agent.types.event.common.Channel channel =
                        channelMap.get(getChannelUniqueName((EndpointImpl) endpoint));
                if (channel != null) {
                    ClientToChannelRelationship clientToChannelRelationship = new ClientToChannelRelationship();
                    clientToChannelRelationship.setChannelId(channel.getId());
                    clientToChannelRelationship.setClientId(commonClient.getId());
                    clientToChannelRelationships.add(clientToChannelRelationship);
                }
            }
        }
    }

    private String getChannelUniqueName(Channel channel) {
        return channel.getType().name() + "-" + channel.getName();
    }

    public com.event.discovery.agent.types.event.common.Client map(Client clientName) {
        com.event.discovery.agent.types.event.common.Client client = new com.event.discovery.agent.types.event.common.Client();
        client.setId(idGenerator.generateRandomUniqueId("1"));
        client.setName(clientName.getName());
        client.setType("consumer");
        client.setAdditionalAttributes(new HashMap<>());
        client.getAdditionalAttributes().put("clientUsername", clientName.getClientUsername().getName());
        client.getAdditionalAttributes().put("clientType", "clientApplication");
        return client;
    }

    public Broker map(SolaceBrokerIdentity brokerIdentity) {
        Broker broker = new Broker();
        broker.setBrokerType("solace");
        if (brokerIdentity != null) {
            if (!StringUtils.isEmpty(brokerIdentity.getBrokerAlias())) {
                broker.setBrokerName(brokerIdentity.getBrokerAlias());
            } else {
                broker.setBrokerName(brokerIdentity.getClientHost());
            }
            broker.setAdditionalAttributes(new HashMap<>());
            broker.getAdditionalAttributes().put("msgvpn", brokerIdentity.getMessageVpn());
        }

        return broker;
    }

    public com.event.discovery.agent.types.event.common.Subscription map(Subscription subscription) {
        com.event.discovery.agent.types.event.common.Subscription commonSubscription =
                new com.event.discovery.agent.types.event.common.Subscription();
        commonSubscription.setId(idGenerator.generateRandomUniqueId("1"));
        commonSubscription.setMatchCriteria(subscription.getSubscription());
        commonSubscription.setAdditionalAttributes(new HashMap<>());
        commonSubscription.getAdditionalAttributes().put("exported", subscription.isExported());
        if (!StringUtils.isEmpty(subscription.getSharedName())) {
            commonSubscription.getAdditionalAttributes().put("shareName", subscription.getSharedName());
        }
        return commonSubscription;
    }

    public com.event.discovery.agent.types.event.common.Channel map(Channel channel) {
        com.event.discovery.agent.types.event.common.Channel commonChannel =
                new com.event.discovery.agent.types.event.common.Channel();
        commonChannel.setId(idGenerator.generateRandomUniqueId("1"));
        commonChannel.setName(channel.getName());
        commonChannel.setAdditionalAttributes(new HashMap<>());
        commonChannel.getAdditionalAttributes().put("type", channel.getType().name().toLowerCase(Locale.getDefault()));
        if (channel instanceof Endpoint) {
            commonChannel.getAdditionalAttributes().put("durable", ((Endpoint) channel).isDurable());
            commonChannel.getAdditionalAttributes().put("exclusive", ((Endpoint) channel).isExclusive());
        }
        return commonChannel;
    }
}
