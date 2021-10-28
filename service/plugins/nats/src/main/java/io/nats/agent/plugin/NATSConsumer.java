package io.nats.agent.plugin;

import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.model.DiscoveryOperation;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedEvent;
import com.event.discovery.agent.framework.model.messages.BasicNormalizedMessage;
import io.nats.agent.plugin.model.NATSIdentity;
import io.nats.client.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class NATSConsumer {
    NATSIdentity identity;
    DiscoveryOperation discoveryOperation;
    NATSPlugin plugin;
    Connection nc;
    Dispatcher dispatcher;

    public NATSConsumer(NATSIdentity identity,
                        DiscoveryOperation discoveryOperation,
                        NATSPlugin plugin) {
        this.identity = identity;
        this.discoveryOperation = discoveryOperation;
        this.plugin = plugin;
    }

    public void startSubscribers(BrokerExceptionHandler brokerExceptionHandler) {
        Options options = new Options.Builder()
                .server(String.format("%s://%s:%d", identity.getClientProtocol(), identity.getHostname(), identity.getClientPort()))
                .connectionTimeout(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(10))
                .reconnectWait(Duration.ofSeconds(1))
                .build();

        try {
            nc = Nats.connect(options);
            dispatcher = nc.createDispatcher(plugin::queueReceivedMessage);
            discoveryOperation.getSubscriptionSet().forEach(dispatcher::subscribe);
            nc.flush(Duration.ZERO);
        } catch (Exception e) {
            brokerExceptionHandler.handleException(e);
        }

    }

    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        var natsMsg = (Message)vendorSpecificMessage;
        BasicNormalizedEvent event = new BasicNormalizedEvent();
        BasicNormalizedMessage msg = new BasicNormalizedMessage();
        event.setNormalizedMessage(msg);

        // Set the topic name
        var topic = natsMsg.getSubject();
        event.setChannelName(topic);
        event.setChannelType("topic");
        msg.setPayload(natsMsg.getData());

        return event;
    }


    public void stopSubscribers() {
        try {
// Messages that have arrived will be processed
            CompletableFuture<Boolean> drained = dispatcher.drain(Duration.ofSeconds(10));
// Wait for the drain to complete
            drained.get();
// Close the connection
            nc.close();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }
}
