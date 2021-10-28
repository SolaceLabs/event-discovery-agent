package com.rabbitmq.agent.plugin;


import com.rabbitmq.agent.plugin.model.RabbitMQIdentity;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Slf4j
@Component
@Scope(SCOPE_PROTOTYPE)
public class RabbitMQRuntimeListener {
    private Connection rabbitConnection;
    private DiscoveryOperation discoveryOperation;
    private AbstractBrokerPlugin plugin;
    private Client rabbitClient;
    private Map<String, Channel> queueCreatedMap = new HashMap<>();

    public void configure(RabbitMQIdentity identity, DiscoveryOperation discoveryOperation, AbstractBrokerPlugin plugin)
            throws IOException, TimeoutException, URISyntaxException {
        this.discoveryOperation = discoveryOperation;
        this.plugin = plugin;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(identity.getHostname());
        factory.setPort(Integer.valueOf(identity.getClientPort()));
        factory.setUsername(identity.getClientUsername());
        factory.setPassword(identity.getClientPassword());

        rabbitConnection = factory.newConnection();
        rabbitClient = new Client(
                new ClientParameters()
                        .url("http://" + identity.getHostname() + ":" + identity.getAdminPort() + "/api/")
                        .username(identity.getClientUsername())
                        .password(identity.getClientPassword())
        );
    }

    public void subscribe() throws IOException {

        if (discoveryOperation.getSubscriptionSet() == null) {
            List<ExchangeInfo> exchangeList = rabbitClient.getExchanges();
            for (ExchangeInfo exchange : exchangeList) {
                if (exchange.getType().compareTo("topic") == 0) {
                    Channel channel = rabbitConnection.createChannel();
                    String queueName = channel.queueDeclare().getQueue();
                    queueCreatedMap.put(queueName, channel);
                    channel.queueBind(queueName, exchange.getName(), "#");
                    boolean autoAck = true;
                    log.info("Binding: " + queueName + " with Exchange: " + exchange.getName());
                    channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {
                    });
                }
            }
        }
    }

    private DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String routingKey = delivery.getEnvelope().getRoutingKey();
        //String exchangeID = delivery.getEnvelope().getExchange();
        System.out.println(" [x] Received '" + routingKey + "'");
        plugin.queueReceivedMessage(delivery);
    };

    public void finish() throws IOException, TimeoutException {
        for (String queue : queueCreatedMap.keySet()) {
            Channel channel = queueCreatedMap.get(queue);
            channel.queueDelete(queue);
        }
        rabbitConnection.close();
    }


}
