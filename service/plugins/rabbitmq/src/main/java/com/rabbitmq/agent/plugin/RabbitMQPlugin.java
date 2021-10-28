package com.rabbitmq.agent.plugin;

import com.rabbitmq.agent.plugin.model.RabbitMQIdentity;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import com.rabbitmq.http.client.domain.BindingInfo;
import com.rabbitmq.http.client.domain.ChannelInfo;
import com.rabbitmq.http.client.domain.ConsumerDetails;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.QueueInfo;
import com.event.discovery.agent.types.event.common.Channel;
import com.event.discovery.agent.types.event.common.ChannelToChannelRelationship;
import com.event.discovery.agent.types.event.common.ChannelToSubscriptionRelationship;
import com.event.discovery.agent.types.event.common.ClientToChannelRelationship;
import com.event.discovery.agent.types.event.common.DiscoveryData;
import com.event.discovery.agent.types.event.common.DiscoveryEntity;
import com.event.discovery.agent.types.event.common.Subscription;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.CommonModelConstants;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import com.event.discovery.agent.framework.model.broker.NoAuthBrokerAuthentication;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedEvent;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedMessage;
import com.event.discovery.agent.framework.utils.IDGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RabbitMQPlugin extends AbstractBrokerPlugin implements VendorBrokerPlugin {

    @Autowired
    public RabbitMQPlugin(MeterRegistry meterRegistry, IDGenerator idGenerator, RabbitMQRuntimeListener listener) {
        super(meterRegistry);
        this.idGenerator = idGenerator;
        this.listener = listener;
    }

    private final IDGenerator idGenerator;
    private final RabbitMQRuntimeListener listener;

    @Override
    public BrokerConfigData getBrokerConfig() {
        Client rabbitClient = null;
        try {
            RabbitMQIdentity identity = (RabbitMQIdentity) getBrokerIdentity();

            rabbitClient = new Client(
                    new ClientParameters()
                            .url("http://" + identity.getHostname() + ":" + identity.getAdminPort() + "/api/")
                            .username(Optional.ofNullable(identity.getClientUsername()).orElse(""))
                            .password(Optional.ofNullable(identity.getClientPassword()).orElse(""))
            );

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Configure the listener
        try {
            listener.configure((RabbitMQIdentity) getBrokerIdentity(), getDiscoveryOperation(),
                    this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        DiscoveryData discoveryData = new DiscoveryData();


        List<Channel> queues = discoverQueues(rabbitClient);
        discoveryData.getChannels().addAll(queues);
        log.info("adding channels <Queues> {}", queues);

        List<Channel> exchanges = discoverExchanges(rabbitClient);
        discoveryData.getChannels().addAll(exchanges);
        log.info("adding channels <Exchanges> {}", exchanges);

        List<com.event.discovery.agent.types.event.common.Client> clients = discoverConnections(rabbitClient);
        discoveryData.getClients().addAll(clients);
        log.info("adding clients <Channels> {}", clients);

        List<ChannelToChannelRelationship> bindings = discoverBindings(rabbitClient, discoveryData);
        discoveryData.getChannelToChannelRelationships().addAll(bindings);
        log.info("adding bindings: {}", bindings);


        List<ClientToChannelRelationship> clientToChannelRelationships = discoverConsumers(rabbitClient, discoveryData);
        discoveryData.getClientToChannelRelationships().addAll(clientToChannelRelationships);
        log.info("adding client to channel maps: {}", clientToChannelRelationships);

        List<Subscription> subscriptions = discoverSubscriptions(rabbitClient, discoveryData);
        discoveryData.getSubscriptions().addAll(subscriptions);
        log.info("adding subscriptions: {}", subscriptions);

        List<ChannelToSubscriptionRelationship> channelToSubscriptionRelationships = discoverQueueToRoutingKeyRelationship(discoveryData);
        discoveryData.getChannelToSubscriptionRelationships().addAll(channelToSubscriptionRelationships);
        log.info("adding channel to subscription relationships: {}", channelToSubscriptionRelationships);

        return BrokerConfigData.builder().configuration(Collections.EMPTY_MAP).commonDiscoveryData(discoveryData).build();
    }

    private List<Channel> discoverQueues(Client rabbitClient) {
        List<QueueInfo> rabbitQueues = rabbitClient.getQueues();
        List<Channel> channels = rabbitQueues.stream().map(c -> {
            Channel channel = new Channel();
            channel.setId(idGenerator.generateRandomUniqueId("1"));
            channel.setName(c.getName());
            channel.setAdditionalAttributes(new HashMap<>());
            channel.getAdditionalAttributes().put(CommonModelConstants.Channel.TYPE, "Queue");

            callAllGetApisAndStoreInAdditionalAttributes(c, channel,
                    m -> "getName".compareTo(m.getName()) != 0);
            return channel;

        }).collect(Collectors.toList());

        return channels;

    }

    private List<Channel> discoverExchanges(Client rabbitClient) {
        List<ExchangeInfo> rabbitExchanges = rabbitClient.getExchanges();
        List<Channel> channels = rabbitExchanges.stream().map(c -> {
            Channel channel = new Channel();
            channel.setId(idGenerator.generateRandomUniqueId("1"));
            channel.setName(c.getName());
            channel.setAdditionalAttributes(new HashMap<>());
            channel.getAdditionalAttributes().put(CommonModelConstants.Channel.TYPE, "Exchange." + c.getType());

            callAllGetApisAndStoreInAdditionalAttributes(c, channel,
                    m -> "getName".compareTo(m.getName()) != 0 && "getType".compareTo(m.getName()) != 0);
            return channel;

        }).collect(Collectors.toList());

        return channels;

    }

    private List<com.event.discovery.agent.types.event.common.Client> discoverConnections(Client rabbitClient) {
        List<ChannelInfo> rabbitChannels = rabbitClient.getChannels();
        List<com.event.discovery.agent.types.event.common.Client> clients = rabbitChannels.stream().map(c -> {
            com.event.discovery.agent.types.event.common.Client client = new com.event.discovery.agent.types.event.common.Client();
            client.setId(idGenerator.generateRandomUniqueId("1"));
            client.setName(c.getName());
            client.setAdditionalAttributes(new HashMap<>());

            callAllGetApisAndStoreInAdditionalAttributes(c, client,
                    m -> "getName".compareTo(m.getName()) != 0);
            return client;

        }).collect(Collectors.toList());

        return clients;

    }

    private List<ChannelToChannelRelationship> discoverBindings(Client rabbitClient, DiscoveryData dd) {
        Map<String, Channel> channelMap = dd.getChannels().stream().collect(Collectors.toMap(Channel::getName, Function.identity()));

        List<BindingInfo> rabbitBindings = rabbitClient.getBindings();
        List<ChannelToChannelRelationship> c2cList = rabbitBindings.stream().map(c -> {
            ChannelToChannelRelationship c2c = new ChannelToChannelRelationship();
            c2c.setSourceChannelId(channelMap.get(c.getSource()).getId());
            c2c.setTargetChannelId(channelMap.get(c.getDestination()).getId());
            c2c.setAdditionalAttributes(new HashMap<>());

            callAllGetApisAndStoreInAdditionalAttributes(c, c2c, m -> true);
            return c2c;

        }).collect(Collectors.toList());

        return c2cList;

    }

    private List<ClientToChannelRelationship> discoverConsumers(Client rabbitClient, DiscoveryData dd) {
        List<ConsumerDetails> consumerDetailsList = new ArrayList<>();
        List<ConsumerDetails> rabbitConsumers = rabbitClient.getConsumers();

        for (ConsumerDetails consumerDetails : rabbitConsumers) {
            if (consumerDetails.getQueueDetails() != null) {
                consumerDetailsList.add(consumerDetails);
            }
        }

        Map<String, Channel> channelMap = dd.getChannels().stream().collect(Collectors.toMap(Channel::getName, Function.identity()));
        Map<String, com.event.discovery.agent.types.event.common.Client> clientMap =
                dd.getClients().stream()
                        .collect(Collectors.toMap(com.event.discovery.agent.types.event.common.Client::getName, Function.identity()));


        List<ClientToChannelRelationship> c2cList = consumerDetailsList.stream().map(c -> {
            ClientToChannelRelationship c2c = new ClientToChannelRelationship();
            c2c.setClientId(clientMap.get(c.getChannelDetails().getName()).getId());
            clientMap.get(c.getChannelDetails().getName()).setType("consumer");
            c2c.setChannelId(channelMap.get(c.getQueueDetails().getName()).getId());
            return c2c;

        }).collect(Collectors.toList());

        return c2cList;

    }

    private List<Subscription> discoverSubscriptions(Client rabbitClient, DiscoveryData dd) {

        List<BindingInfo> rabbitBindings = rabbitClient.getBindings();
        Map<String, Channel> channelMap = dd.getChannels().stream().collect(Collectors.toMap(Channel::getName, Function.identity()));

        log.trace("Channel map has {} entries", channelMap.entrySet().size());

        List<Subscription> subscriptionsList = rabbitBindings.stream().map(c -> {
            Subscription sub = new Subscription();
            sub.setId(idGenerator.generateRandomUniqueId("1"));
            sub.setMatchCriteria(c.getRoutingKey());
            sub.setAdditionalAttributes(new HashMap<>());

            callAllGetApisAndStoreInAdditionalAttributes(c, sub, m -> true);

            return sub;

        }).collect(Collectors.toList());

        return subscriptionsList;

    }

    private List<ChannelToSubscriptionRelationship> discoverQueueToRoutingKeyRelationship(DiscoveryData dd) {
        List<Subscription> subscriptionList = dd.getSubscriptions();
        Map<String, Channel> channelMap = dd.getChannels().stream().collect(Collectors.toMap(Channel::getName, Function.identity()));

        List<ChannelToSubscriptionRelationship> subscriptionRelationships = subscriptionList.stream().map(c -> {
            ChannelToSubscriptionRelationship relationship = new ChannelToSubscriptionRelationship();
            relationship.setSubscriptionId(c.getId());
            relationship.setChannelId(channelMap.get(c.getAdditionalAttributes().get("Destination")).getId());
            return relationship;
        }).collect(Collectors.toList());

        return subscriptionRelationships;
    }

    private void callAllGetApisAndStoreInAdditionalAttributes(Object c,
                                                              DiscoveryEntity entity,
                                                              Predicate<Method> customPredicate) {
        Class<?> clazz = c.getClass();
        Method[] allMethods = clazz.getDeclaredMethods();
        log.info(allMethods.toString());
        for (Method m : allMethods) {
            m.setAccessible(true);
            Object result;
            try {
                if (m.getName().startsWith("get") && m.isVarArgs() == false) {
                    if (customPredicate.test(m)) {
                        result = m.invoke(c);
                        String name = m.getName().replaceFirst("get", "");
                        if (result != null) {
                            entity.getAdditionalAttributes().put(name, result.toString());
                        } else {
                            entity.getAdditionalAttributes().put(name, null);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    @Override
    public void startSubscribers(BrokerExceptionHandler brokerExceptionHandler) {
        try {
            listener.subscribe();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        if (vendorSpecificMessage instanceof Delivery) {
            Delivery rabbitmsg = (Delivery) vendorSpecificMessage;
            if (rabbitmsg.getBody() != null) {
                BasicNormalizedEvent event = new BasicNormalizedEvent();
                BasicNormalizedMessage msg = new BasicNormalizedMessage();
                event.setNormalizedMessage(msg);

                log.info("Routing Key: " + rabbitmsg.getEnvelope().getRoutingKey());
                event.setChannelName(rabbitmsg.getEnvelope().getRoutingKey());
                event.setChannelType("topic");
                Map<String, Object> map = new HashMap<>();
                map.put("exchange", rabbitmsg.getEnvelope().getExchange());
                event.setAdditionalParameters(map);
                msg.setPayload(rabbitmsg.getBody());

                return event;
            }
        }
        return null;
    }

    @Override
    public void stopSubscribers() {
        try {
            listener.finish();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void closeSession() {

    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .pluginName("RabbitMQ")
                .brokerCapabilities(getBrokerCapabilities())
                .inputSchema(new HashMap<>())
                .brokerAuthenticationClass(NoAuthBrokerAuthentication.class)
                .brokerIdentityClass(RabbitMQIdentity.class)
                .brokerType("RABBITMQ")
                .pluginClass(getClass())
                .build();
    }

    @Override
    protected BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        Set<BrokerCapabilities.Capability> capabilities = brokerCapabilities.getCapabilities();
        capabilities.add(BrokerCapabilities.Capability.TIMED_MESSAGE_CONSUMER);
        return brokerCapabilities;
    }
}