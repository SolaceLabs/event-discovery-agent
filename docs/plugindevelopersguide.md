# Plugin Developer's Guide

## Prerequisites:
* Java 11 JDK
* Maven 3.2+
* your favorite IDE (which ideally has plugins for Lombok and MapStruct)

## Get the Code
```
git clone https://github.com/SolaceDev/event-discovery-agent.git
```

## Compile/Build
```
cd event-discovery-agent/service
mvn clean install
```

## Running the agent
**Command Line**
```
java -jar services/eventDiscoveryService/target/event-discovery-agent-app-1.0-SNAPSHOT.jar
```

**IDE**

Run the class `com.event.discovery.agent.EventDiscoveryAgentApplication` in the `service/services/eventDiscoveryService` directory.

## Implementing Your Own Custom Plugin

In this section, we will create a plugin for the HiveMQ message broker.

### Create the plugin scaffolding
Create a new directory under the `event-discovery-agent/service/plugins` directory and create the base java package directory:

From the `event-discovery-agent` root directory:

```
mkdir -p service/plugins/hiveMQ/src/main/java/com/hivemq/agent/plugin
```

#### Create a starter pom.xml file:

Create a file service/plugins/hiveMQ/pom.xml with the following content:

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.event.discovery.agent.plugins</groupId>
    <artifactId>hivemq-plugin</artifactId>
    <version>0.0.1</version>
    <name>Event Discovery Agent - HiveMQ Plugin</name>
    <description>HiveMQ Event Discovery Plugin</description>

    <parent>
        <groupId>com.event.discovery.agent.plugins</groupId>
        <artifactId>agent-plugins</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>com.event.discovery.agent.libraries</groupId>
            <artifactId>framework-plugin</artifactId>
            <version>0.0.1</version>
        </dependency>
        <dependency>
            <groupId>com.hivemq</groupId>
            <artifactId>hivemq-mqtt-client</artifactId>
            <version>1.2.2</version>
        </dependency>
    </dependencies>

</project>
```

Be sure to include the dependencies required for the broker's administrative and messaging client APIs (this can be done later if you don't know them at this point).

#### Add the new plugin module to the parent module
Edit the `service/plugins/pom.xml` and add the new HiveMQ module to the list of modules:

```
<modules>
    <module>solace</module>
    <module>rabbitmq</module>
    <module>nats</module>
    <module>google</module>
    <module>kafkaApache</module>
    <module>kafkaConfluent</module>
    <module>hivemq</module>
</modules>
```

### Add the new plugin to the service pom.xml
Edit the service/services/eventDiscoveryService/pom.xml file and add the following dependency

```
<dependency>
    <groupId>com.event.discovery.agent.plugins</groupId>
    <artifactId>hivemq-plugin</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Create an empty plugin
Create the file `service/plugins/hiveMQ/src/main/java/com/hivemq/agent/plugin/HiveMQPlugin.java`

with the following content:
```
package com.hivemq.agent.plugin;

import com.event.discovery.agent.framework.AbstractBrokerPlugin;
import com.event.discovery.agent.framework.VendorBrokerPlugin;
import com.event.discovery.agent.framework.exception.BrokerExceptionHandler;
import com.event.discovery.agent.framework.model.BasicPluginConfigData;
import com.event.discovery.agent.framework.model.BrokerConfigData;
import com.event.discovery.agent.framework.model.NormalizedEvent;
import com.event.discovery.agent.framework.model.PluginConfigData;
import com.event.discovery.agent.framework.model.broker.BrokerCapabilities;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HiveMQPlugin extends AbstractBrokerPlugin implements VendorBrokerPlugin {

    @Autowired
    public HiveQPlugin(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }

    @Override
    public BrokerConfigData getBrokerConfig() {
        return BrokerConfigData.builder()
                .configuration(Collections.EMPTY_MAP)
                .commonDiscoveryData(null)
                .build();
    }

    @Override
    public void startSubscribers(BrokerExceptionHandler brokerExceptionHandler) {
    }

    @Override
    public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
        return null;
    }

    @Override
    public void stopSubscribers() {
    }

    @Override
    public void closeSession() {

    }

    @Override
    public PluginConfigData getPluginConfiguration() {
        return BasicPluginConfigData.builder()
                .build();
    }

    @Override
    protected BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        return brokerCapabilities;
    }
}
```

### Connect to the Broker
Now that scaffolding is in place, let’s add the logic to connect to the HiveMQ broker.

The plugin uses two interfaces that can be extended to add the attributes required to identify and authenticate with the target broker:

```
com.event.discovery.agent.framework.model.broker.BrokerAuthentication
com.event.discovery.agent.framework.model.broker.BrokerIdentity
```

#### Broker Identity
By default, the `BasicBrokerIdentity` class is used for the broker identity fields. If the plugin author wishes to add identifying attributes, they can create their own Identity class that implements `BrokerIdentity` and add customized deserialization logic to `BrokerIdentityDeserializer`.

#### Broker Authentication
By default, the `NoAuthBrokerAuthentication` class is used for broker authentication. If the plugin author wishes to add authentication attributes, they can create their own Authentication class that implements `BrokerAuthentication` and then add customized deserializer logic to `BrokerAuthenticationDeserializer`.

#### Connecting to HiveMQ
For the HiveMQ plugin, we are adding a custom BrokerIdentity class, but our broker does not have authentication enabled, so we are using the default `BrokerAuthentication` authentication class.

#### Custom Broker Identity
Create a new package in the HiveMQ plugin module:

```
com.hivemq.agent.plugin.model
```

Create a new class `HiveMQIdentity` in this package:
```
package com.hivemq.agent.plugin.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.event.discovery.agent.framework.model.broker.BasicBrokerIdentity;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonDeserialize(as = HiveMQIdentity.class)
public class HiveMQIdentity extends BasicBrokerIdentity implements BrokerIdentity {
    private String hostname;
    private String clientProtocol;
    private int clientPort;
    private int adminPort;
}
```

### Configuring the Plugin
The plugin should implement the method getPluginConfiguration where it provides its configuration data to the framework layer above. Add the following method to the `HiveMQPlugin` class:

```
@Override
public PluginConfigData getPluginConfiguration() {
    return BasicPluginConfigData.builder()
            .pluginName("HiveMQ")
            .brokerCapabilities(getBrokerCapabilities())
            .inputSchema(new HashMap<>())
            .brokerAuthenticationClass(NoAuthBrokerAuthentication.class)
            .brokerIdentityClass(HiveMQIdentity.class)
            .brokerType("HIVEMQ")
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
```

The plugin provides the following configuration data to the framework layer:

* `pluginName` - The name of the plugin
* `brokerCapabilities` - Instructs the layers above how we want to gather the data from the broker. More information on capabilities is given below.
* `inputSchema` - Not used at the moment but allows the plugin author to provide metadata on the plugin
* `brokerAuthenticationClass` - The class that the deserializer uses to deserialize the authorization component of incoming requests
* `brokerIdentityClass` - The class that the deserializer uses to deserialize the identity component of incoming requests
* `brokerType` - The broker type for this plugin, all uppercase by convention.
* `pluginClass` - The class that the pluginFactory instantiates and invokes when handling a request for this brokerType.

### Collecting Data from the Broker
There are 2 phases to data collection:

* `phase 1`: Configuration data - this includes clients, persistent channels (queues), subscriptions and connectors (and schemas if you are using a schema registry)
* `phase 2`: Runtime data - this includes dynamic topics and message payloads

### Phase 1 - Broker Configuration Data
The broker's configuration data is collected by the plugin in the getBrokerConfig method and returned in the format BrokerConfigData

Here is some example code that collects clients and subscriptions from a HiveMQ broker using the admin API:
```
@Override
public BrokerConfigData getBrokerConfig() {
    hiveMQAdmin.initialize((HiveMQIdentity) getBrokerIdentity());

    // Configure the listener
    listener.configure((HiveMQIdentity) getBrokerIdentity(), getDiscoveryOperation(),
            this);

    DiscoveryData discoveryData = new DiscoveryData();

    // Get Clients
    HiveMQClientList clientList = hiveMQAdmin.getClientList();
    List<Client> clients = clientList.getItems().stream()
            .map(c -> {
                Client client = new Client();
                client.setId(idGenerator.generateRandomUniqueId("1"));
                client.setName(c.getId());
                client.setType("");
                return client;
            })
            .collect(Collectors.toList());
    discoveryData.getClients().addAll(clients);
    log.info("Adding clients {}", clients);

    // Get subscriptions
    clients.stream()
            .forEach(client -> getSubscriptions(client, discoveryData));
    return BrokerConfigData.builder()
            .configuration(Collections.EMPTY_MAP)
            .commonDiscoveryData(discoveryData)
            .build();
}

private void getSubscriptions(Client client, DiscoveryData discoveryData) {
    HiveMQSubscriptionList subList = hiveMQAdmin.getSubscriptionList(client.getName());
    subList.getItems().stream()
            .forEach(sub -> {
                // Add subscription to discovery data
                Subscription newSub = new Subscription();
                newSub.setId(idGenerator.generateRandomUniqueId("1"));
                newSub.setMatchCriteria(sub.getTopicFilter());
                discoveryData.getSubscriptions().add(newSub);

                // Add client to subscription relationship
                ClientToSubscriptionRelationship clientSub = new ClientToSubscriptionRelationship();
                clientSub.setClientId(client.getId());
                clientSub.setSubscriptionId(newSub.getId());
                discoveryData.getClientToSubscriptionRelationships().add(clientSub);
            });
}
```

The above code creates client and subscription entities and clientToSubscription entities which link the two entity types.

### Phase 2 - Runtime Listener
The plugin controls the lifecycle of the runtime listener through the following methods:

`public void startSubscribers(BrokerExceptionHandler brokerExceptionHandler)` - used to start the listener. This generally involves connecting to the broker, setting up subscriptions and registering a message handling callback (or polling for brokers such as Kafka).

`public void stopSubscribers()` - used to stop the listener. Generally used to remove subscriptions.

`public void closeSession()` - used to tear down resources associated with runtime listening

#### Message Processing
As messages are received by the message handler, they are handed off to the plugins queueReceivedMessage method:

```
        client.subscribeWith()
                .topicFilter(subscription)
                .callback(plugin::queueReceivedMessage)
                .send();
```

#### Message Normalization
The plugin is responsible for normalizing each message using the `NormalizedEvent` interface.

The `NormalizedEvent` contains:
* the channel / topic name
* the schema (when there is a schema registry)
* a `NormalizedMessage` (when there is no schema registry)

The `NormalizedMessage` contains the message payload and other message metadata (message size, send/receive timestamps).

Here is the normalizeMessage method in the plugin:

```
@Override
public NormalizedEvent normalizeMessage(Object vendorSpecificMessage) {
    if (vendorSpecificMessage instanceof Mqtt3Publish) {
        Mqtt3Publish mqttMessage = (Mqtt3Publish) vendorSpecificMessage;
        if (mqttMessage.getPayload().isPresent()) {
            ByteBuffer message = mqttMessage.getPayload().get();

            BasicNormalizedEvent event = new BasicNormalizedEvent();
            BasicNormalizedMessage msg = new BasicNormalizedMessage();
            event.setNormalizedMessage(msg);

            // Set the topic name
            event.setChannelName(mqttMessage.getTopic().toString());
            event.setChannelType("topic");

            // Copy the payload
            byte[] bytes = new byte[message.remaining()];
            message.get(bytes);

            msg.setPayload(bytes);
            return event;
        }
    }
    return null;
}
```

### BrokerCapabilities
`BrokerCapabilities` provide a way for plugins to communicate their capabilities to the application layer. For example here is the getBrokerCapabilities method for the HiveMQPlugin:

```
@Override
    public BrokerCapabilities getBrokerCapabilities() {
        BrokerCapabilities brokerCapabilities = new BrokerCapabilities();
        brokerCapabilities.setCapabilities(new HashSet<>(
                Arrays.asList(BrokerCapabilities.Capability.TIMED_MESSAGE_CONSUMER)));
        return brokerCapabilities;
    }
```

The `TIMED_MESSAGE_CONSUMER` capability instructs the application to call the plugins message listener lifecycle methods to capture messages at runtime.

The `NO_SCHEMA_INFERENCE` capability instructs the application not to infer schemas from the payload (only topics/channels are returned).

The `SCHEMA_REGISTRY` capability instructs the application fully normalized schemas are already in the normalizedEvents sent from the plugin.

### ComponentScan
Springboot needs to find the new plugin components using the component scan. By default, Spring finds all Spring components under the `com.event.discovery.agent` package. If other packages require scanning, the need to be added to the `EventDiscoveryAgentApplication` `@ComponentScan` annotation. For example, for the HiveMQ plugin, the `com.hivemq` package is added:

```
@ComponentScan(basePackages = {"com.event.discovery.agent", "com.hivemq"}, ....
```

The internal data model used by the Event Discovery Agent is very rich and is capable of modelling clients/applications, channels (queues / topics) and routers such as RabbitMQ exchanges. For more information on the internal data model, see [internal data model](./internaldatamodel.md).
