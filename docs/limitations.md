# Limitations

## Apache Kafka Plugin
* Only a single connector cluster can be specified.
* Only the last message on each topic is examined for schema inference. It would be better to examine multiple messages to get more accurate representation of the event's schema.
* Regex topics are not supported.
* Kafka keys are gathered by the agent but not included in the AsyncAPI output.

## "Dynamic Topic" Plugin
This section applies to plugins that collect topic and payload data on brokers that support dynamic topic structures.
* the number of unique topics may be very large. Topic best practices dictate that topic structure should include information such as object identifies, latitude/longitude, or any dynamic data that helps the consumer receive the desired events. Ideally, the framework would allow the user to identify levels in the topic hierarchy that could be "collapsed" and to insert appropriate variables in their place. This is colloquially known as "topic squashing" and provides additional insight into the structure and use of the topic hierarchy.

## Confluent Kafka Plugin
* Only the last schema from each subject is gathered at the moment. AsyncAPI doesn't support versioned schemas out of the box (as of 2.2.0) without hacks such as appending the version to the identifying attribute. As AsyncAPI evolves in capability, so can the plugin.

## Nats, RabbitMQ and HiveMQ Plugins
These plugins are "demo" quality and not extensively tested. They represent a good starting point for more feature-rich and hardened plugins.

## General
* For now, all AsyncAPI operations are "publish". The event discovery agent considers itself a consumer, and in the confusing AsyncAPI aspect reversal, this is considered a "publish" operation from the messaging broker.
* The agent only outputs the AsyncAPI specification in json format.
* There are no bindings added to operations, servers, channels or operations. This is an obvious area for enhancement where each plugin could provide it's own AsyncAPI bindings via a common interface.
* No servers are added to the AsyncAPI output. This is another obvious area for enhancement.
* The common schema inference mechanism is limited and could be replaced by open-source tooling.
* The AsyncAPI specification version is currently limited to a subset of the 2.2.0 spec. The infrastructure is in place to add previous or future versions.
* The current [internal data model](./internaldatamodel.md) may be superseded by a more expressive model in the future.

# Future Enhancements
Only a subset of the data collected by the Event Discovery Agent is currently surfaced in the generated AsyncAPI document. This could be somewhat remedied by additional plugin support for adding AsyncAPI bindings. Ideally, the AsyncAPI specification evolves to include standalone "actors" in the data model.

As plugins mature and take on their own lifecycle, it may be adventageous to create separate repositories for each plugin. SpringBoot supports a composable model where plugins can be dynamically recognized at runtime in the classpath, allowing an event discovery agent user to mix and match various external plugins.
