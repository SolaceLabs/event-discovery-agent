# Introduction

Have you ever wondered which events are flowing through your message broker? If so, then the Event Discovery Agent is here to help you out! Simple point the agent at your broker, run a scan, and retrieve an AsyncAPI document describing the events.

# What brokers are supported?

The current versions has well tested plugins that support Apache Kafka and Solace PubSub+ brokers, plus a plugin to scan a Confluent Schema Registry. There are also untested plugins which support Nats, RabbitMQ and HiveMQ brokers.

# Wait, this is free?

Yep, free, open-source, Apache 2.0.

# What about XYZ broker?

Glad you asked, the Event Discovery Agent is built with an extendable plugin architecture. The plugin developer's guide will guide you through creating a plugin for your XYZ broker.

# I'm sold! How do I use it?

The Agent has a REST API that is used to initiate an asynchronous broker scan and then retrieve the AsyncAPI results. For more information on the REST API, see the rest api documentation. Here is an example of how to initiate an Apache Kafka Discovery:
```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "kafka1:12091",
        },
        "brokerAuthentication": {
        	"brokerType": "KAFKA",
        	"authenticationType": "NOAUTH",
        	"transportType": "PLAINTEXT",
        },
        "discoveryOperation": {
          "operationType": "eventDiscovery",
          "messageQueueLength": 100000,
          "durationInSecs": 8,
          "subscriptionSet": [
          ],
          "name": "Kafka scan"
        }
}
```

# Sorry, I'm not a techie, REST APIs are too hard for me. Is there another way to interact with the Agent?

Np worries, there is a built in user interface for the well tested plugins (Apache Kafka, Confluent SchemaRegistry + Kafka and Solace PubSub+). You can access the user interface at http://localhost:8120 after starting the agent.

[insert screenshot of Kafka UI]

# So how does the event collection actually work?

This depends on the plugin. For Apache Kafka, a message listener pulls the last record from each topic and reverse-engineers a JsonSchema representation of the payload (obviously only works for json payload). For the confluent registry plugin, the agent pulls the latest schema from each subject and depending on the SubjectNameStrategy determines the topic the schema belongs to. For all other plugins (Nats, RabbitMQ, HiveMQ and Solace PubSub+) the agent creates a passive message listener that attracts message depending on the supplied subscriptions. The last message on each topic is used to create a JsonSchema representation of the payload (again, only works for json payload).

# This sounds too good to be true, what are the limitations?

As you can imagine, being a hackathon project, the Agent does have [limitations].
