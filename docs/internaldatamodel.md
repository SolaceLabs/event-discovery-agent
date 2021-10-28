# Internal Data Model

## Objective
Create the definition for a model that correctly and concisely represents the objects and relationships that determine the messaging flow in a single messaging broker. The model definition must be generic and be extendable to new brokers. As the AsyncAPI spec evolves it may be adopted as the internal model but the current channel-centric spec is not suitable for representing a broker's internal messaging components. The model put forth in this proposal could however be translated to AsyncAPI.

## Approach
The common model attempts to strike a balance between being too generic (sets of key/value pairs) and being too specific (objects tailored exactly to a broker) by defining the broker objects and object relationships at a high enough level to be meaningful in an EDA context but precise enough that they can be extended for specific broker implementations. The conceit with this approach is that when brokers stray too far from the traditional EDA paradigm this may require changes/additions to the model to be adequately represented. To help mitigate this, a breadth of four broker types are considered in the creation of this model: Kafka, Solace, IBM MQ and RabbitMQ.

## Out of scope
For the moment, the following considerations are out of the scope of the model:

* Authorization
* Inter-broker communication

## Top-Level Objects
The top-level objects are abstractions chosen to represent EDA concepts and objects. The top-level objects described below are a combination of normalized/generic attributes and attributes customized to a broker type. The customized attributes are stored in the additionalAttributes map.

All objects have an id field that is omitted from most of the examples below for the sake of brevity. Ids are unique within their object type.

## Top-level objects include:

* broker
* client
* channel
* subscription
* schema

## Broker
A broker represents an instance of a single messaging broker, or “virtual broker” for multi-tenant brokers. This model does not attempt to represent a network of connected messaging brokers. Each broker would be represented by its own broker object and associated top-level objects/relationships. The objects and relationships could be stitched together by a higher-level application, but that is out of the scope of this model definition. The broker type attribute applies to all instances of objects and relationships in the discovery.

* Kafka: Broker
* Solace: VPN
* IBMMQ: Queue Manager
* RabbitMQ: Virtual Host

#### "Generic" Broker
```
"broker": {
  "brokerType": "generic",
  "hostname": "34.23.54.65"
}
```
#### Kafka Broker
```
"broker": {
  "brokerType": "kafka",
  "hostname": "myKafkaBroker.kafka.com"
}
```
#### Solace Broker
```
"broker": {
  "brokerType": "solace",
  "hostname": "mySolaceHost.solace.com"
  "additionalAttributes": {
    "vpnName": "myVpnName"
  }
}
```

#### IBMMQ Broker
```
"broker": {
  "brokerType": "ibmmq",
  "hostname": "myIBMMQHost.ibm.com"
  "additionalAttributes": {
    "queueManager": "QM1"
  }
}
```
#### RabbitMQ Broker
```
"broker": {
  "brokerType": "rabbitmq",
  "hostname": "myRabbitMQHost.rabbitmq.com"
  "additionalAttributes": {
    "virtualHost": "vm1"
  }
}
```

## Client
An active or inactive messaging client in the Broker. A client has a top-level type attribute which is used to indicate if the client is a consumer, or producer. This client type implies message flow direction in the context of relationships with channel objects.

* Kafka: consumer group
* Solace: clientname
* IBMMQ: connection
* RabbitMQ: channel

#### Generic Client
```
"client": {
  "name": "myClientName",
  "type": "producer"
}
```
#### Kafka Consumer Group
```
"client": {
  "name": "myKafkaGroupId",
  "type": "consumer",
  "additionalAttributes": {
    "clientType": "clientApplication"
    "simpleConsumer": false
  }
}
```
#### IBM MQ Client
```
"client": {
  "name": "myAppName",
  "type": "consumer",
  "additionalAttributes": {
    "clientType": "clientApplication",
    "user": "myUsername"
  }
}
```
#### Solace Client
```
"client": {
  "name": "myClientName",
  "type": "consumer",
  "additionalAttributes": {
    "clientType": "clientApplication",
    "clientUsername": "myClientUsername",
    "clientProfileName": "myClientProfileName",
    "aclProfileName": "myAclProfileName"
  }
}
```

## Connectors
Connectors are also modelled as clients, with custom properties for the connector properties.

Kafka Connector

```
"client": {
  "name": "connectorName",
  "type": "producer",
  "additionalAttributes": {
    "clientType": "connector"
    "connectorClass": "com.confluent.fileInputConnector",
    "maxThreads": 1
  }
}
```

## Channel
A channel represents the broker entities that are responsible for the transmission and storage of messages.

Types include topic, queue and broker specific hybrid objects (Solace: topicEndpoint, IBMMQ: topicObject).

#### Kafka Topic (and generic topic)

```
"channel": {
  "name": "kafkaTopicName",
  "additionalAttributes": {
    "type": "topic"
  }
}
```
#### Solace Topic
```
"channel": {
  "name": "solaceTopicName",
  "additionalAttributes": {
    "type": "topic"
  }
}
```
#### Solace Queue
```
"channel": {
  "name": "solaceQueueName",
  "additionalAttributes": {
    "type": "queue",
    "durable": true,
    "exclusive": false
  }
}
```
#### Solace Topic Endpoint
```
"channel": {
  "name": "solaceQueueName",
  "additionalAttributes": {
   "type": "topicEndpoint",
    "durable": false,
    "exclusive": true
  }
}
```
#### IBMMQ Topic Object
```
"channel": {
  "name": "topicObjectName",
  "additionalAttributes": {
    "type": "topicObject",
    "topicString": "my/topic/string",
    "accessControl": {
       .....
    }
  }
}
```
#### RabbitMQ/AMQP Exchange
```
"channel": {
  "name": "exchangeName",
  "additionalAttributes": {
    "type": "exchange",
    "exchangeType": "direct"
  }
}
```

## Subscription
Represents a potentially unbounded “channel space” described by a set of criteria (topic strings with wildcards, routing keys, etc…).

* Kafka: N/A (consumers bind to an exact set of provisioned topics at runtime)
* Solace: client to topic subscription, queue to topic subscription
* IBMMQ: client to topic subscription, unmanaged queue subscription
* RabbitMQ: queue to router binding

Topic subscription (solace, IBM MQ, generic)
```
"subscription": {
  "matchCriteria": "my/topic/space/>"
}
```
RabbitMQ queue to exchange binding
```
"subscription": {
  "matchCriteria": "abc.*.xyz"
}
```

## Schema
The schema of the message payload (and or key). Can be inferred from a message or discovered from a schema registry.

Attributes include:
* content - the schema content
* schemaType - The type of the schema.
* hints - a pre-defined set of hints to help upper-layer applications process the schema.
* primitive - true if the schema is a primitive type.

Schema from AVRO schema registry

```
"schema": {
  "hints": [],
  "schemaType": "AVRO",
  "content": "{\"type\":\"record\",\"name\":\"MQL\",\"namespace\":\"com.myenterprise.sales.kpi\",\"fields\":[{\"name\":\"kpiId\",\"type\":{\"type\":\"record\",\"name\":\"Myid\",\"fields\":[{\"name\":\"myMQLType\",\"type\":[\"null\",\"string\"]}]}},{\"name\":\"LeadId\",\"type\":[\"null\",\"string\"]},{\"name\":\"myApplicationId\",\"type\":[\"null\",\"string\"]},{\"name\":\"leadType\",\"type\":[\"null\",\"string\"]},{\"name\":\"SQL\",\"type\":{\"type\":\"record\",\"name\":\"mySQLObject\",\"fields\":[{\"name\":\"sales\",\"type\":[\"null\",\"string\"]}]}},{\"name\":\"myUserId\",\"type\":[\"null\",\"string\"]},{\"name\":\"msgVersion\",\"type\":[\"null\",\"string\"]},{\"name\":\"myClassName\",\"type\":[\"null\",\"string\"]}]}",
  "primitive": false,
  "additionalAttributes": {
    "subjectName": "MyCompany.Sales.Europe.Leads-com.myenterprise.sales.kpi.MQL",
    "subjectVersion": 1,
    "schemaReg": true
  }
}
```
Inferred schema

```
"schema": {
  "schemaId": "f75qxucyaj",
  "hints": [],
  "schemaType": "JSONSCHEMA",
  "content": "{\n  \"definitions\": \"{}\",\n  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n  \"type\": \"object\",\n  \"title\": \"acme_rideshare_ride_requested_0.0.1\",\n  \"properties\": {\n    \"customer_id\": {\n      \"$id\": \"#/properties/customer_id\",\n      \"title\": \"customer_id schema\",\n      \"type\": \"string\"\n    },\n    \"request_id\": {\n      \"$id\": \"#/properties/request_id\",\n      \"title\": \"request_id schema\",\n      \"type\": \"string\"\n    },\n    \"fare_id\": {\n      \"$id\": \"#/properties/fare_id\",\n      \"title\": \"fare_id schema\",\n      \"type\": \"string\"\n    },\n    \"product_id\": {\n      \"$id\": \"#/properties/product_id\",\n      \"title\": \"product_id schema\",\n      \"type\": \"string\"\n    },\n    \"start_latitude\": {\n      \"$id\": \"#/properties/start_latitude\",\n      \"title\": \"start_latitude schema\",\n      \"type\": \"number\"\n    },\n    \"start_longitude\": {\n      \"$id\": \"#/properties/start_longitude\",\n      \"title\": \"start_longitude schema\",\n      \"type\": \"number\"\n    },\n    \"end_latitude\": {\n      \"$id\": \"#/properties/end_latitude\",\n      \"title\": \"end_latitude schema\",\n      \"type\": \"number\"\n    },\n    \"end_longitude\": {\n      \"$id\": \"#/properties/end_longitude\",\n      \"title\": \"end_longitude schema\",\n      \"type\": \"number\"\n    }\n  }\n}",
  "primitive": false
}
```

## InterDomainConnection
Out of scope.

## Top Level Relationships
A top-level relationship represents a relationship between two or more top-level objects. The top-level objects in the relationship are referenced using ids.

Top level relationships include:

* clientToSubscriptionRelationship
* channelToSubscriptionRelationship
* clientToChannelRelationship
* channelToChannelRelationship
* channelToSchemaRelationship

### ClientToSubscriptionRelationship
Used to represent client topic subscriptions:

```
"clientToSubscriptionRelationship": {
  "clientId": "abc",
  "subscriptionId": "xyz"
}
```
The cardinality is one to one between a client and subscription.

### ChannelToSubscriptionRelationship
Used to represent queue to topic subscription (Solace/IBM MQ) or queue to exchange subscriptions (RabbitMQ).

Queue with topic subscriptions (cardinality 1:1):
```
"channelToSubscriptionRelationship": {
  "channelId": "abc",
  "subscriptionId": "xyz"
}
```
Subscription match criteria for Rabbit MQ/AMQP Queue (cardinality 1:1):

```
"channelToSubscriptionRelationship": {
  "channelId": "abc",
  "subscriptionId": "xyz"
}
```

### ClientToChannelRelationship
Used to represent client to queue or client to topic bindings.

This includes kafka client to topic, kafka connector to topic and client to queue/topic endpoint mappings for other brokers. Cardinality is 1:1 between a client and channel. If a client has multiple channel mappings, this is represented by multiple relationships.

```
"clientToChannelRelationship": {
  "clientId": "abc",
  "channelId": "xyz"
}
```

### ChannelToChannelRelationship
Associates connected event channels. Can be used to represent queue or topic aliasing (IBM MQ), an inter-exchange connection (to an internal exchange) in RabbitMQ or a queue to exchange binding in RabbitMQ.

Inter-exchange connection in RabbitMQ:

```
"channelToChannelRelationship": {
  "sourceChannelId": "abc",  // Original exchange
  "targetChannelId": "xyz"   // Internal exchange
}
```
Queue to exchange binding in RabbitMQ

```
"channelToChannelRelationship": {
  "sourceChannelId": "abc",  // Queue channel
  "targetChannelId": "xyz"   // Exchange channel
}
```

### ChannelToSchemaRelationship
Associates a schema with a channel (topic/queue). Cardinality is 1:1 between a channel and schema. A channel which contains multiple schemas is represented by multiple relationships.

Topic/Queue to Schema (solace, IBM MQ, generic):

```
"channelToSchemaRelationship": {
  "channelId": "abc",
  "schemaId": "xyz"
}
```
Kafka Topic to Key Schema and Value Schema:

```
"channelToSchemaRelationship": {
  "channelId": "abc",
  "schemaId": "xyz",
  "additionalAttributes": {
    "keySchemaId": "jkl"
  }
}
```

## Extending the model
As we attempt to support new brokers there may be concepts that do not fit into the model as defined above. To make the model more extendable, there are two top-level lists  that allow the model to be extended for arbitrary concepts:

additional objects - a list of custom objects

additional relationships - a list of custom relationships between objects

## Examples
Example 1: Kafka client subscribing to a topic with an Avro schema

```
{
  "broker": {
    "brokerType": "kafka",
    "hostname": "34.23.54.65"
  },
  "clients": [
    {
      "id": "123",
      "type": "consumer",
      "name": "myKafkaGroupId",
      "additionalAttributes": {
        "clientType": "clientApplication"
        "simpleConsumer": false
      }
    }
  ],
  "channels": [
    {
      "id": "456",
      "name": "kafkaTopicName",
      "additionalAttributes": {
        "type": "topic"
      }
    }
  ],
  "schemas": [
    {
      "id": "789",
      "hints": [],
      "schemaType": "AVRO",
      "content": "{\"type\":\"record\",\"name\":\"MQL\",\"namespace\":\"com.myenterprise.sales.kpi\",\"fields\":[{\"name\":\"kpiId\",\"type\":{\"type\":\"record\",\"name\":\"Myid\",\"fields\":[{\"name\":\"myMQLType\",\"type\":[\"null\",\"string\"]}]}},{\"name\":\"LeadId\",\"type\":[\"null\",\"string\"]},{\"name\":\"myApplicationId\",\"type\":[\"null\",\"string\"]},{\"name\":\"leadType\",\"type\":[\"null\",\"string\"]},{\"name\":\"SQL\",\"type\":{\"type\":\"record\",\"name\":\"mySQLObject\",\"fields\":[{\"name\":\"sales\",\"type\":[\"null\",\"string\"]}]}},{\"name\":\"myUserId\",\"type\":[\"null\",\"string\"]},{\"name\":\"msgVersion\",\"type\":[\"null\",\"string\"]},{\"name\":\"myClassName\",\"type\":[\"null\",\"string\"]}]}",
      "primitive": false,
      "additionalAttributes": {
        "subjectName": "MyCompany.Sales.Europe.Leads-com.myenterprise.sales.kpi.MQL",
        "subjectVersion": 1,
        "schemaReg": true
      }
    }  
  ],
  "clientToChannelRelationships": [
    {
      "clientId": "123",
      "channelId": "456"
    }
  ],
  "channelToSchemaRelationships": [
    {
      "channelId": "456",
      "schemaId": "789"
    }
  ]
}
```
Example 2: Kafka Connector with multiple topic bindings and a schema

```
{
  "broker": {
    "brokerType": "kafka",
    "hostname": "34.23.54.65"
  },
  "clients": [
    {
      "id": "123",
      "name": "connectorName",
      "type": "consumer",
      "additionalAttributes": {
        "clientType": "connector"
        "connectorClass": "com.confluent.fileInputConnector",
        "maxThreads": 1,
      }
    }
  ],
  "channels": [
    {
      "id": "abc",
      "name": "kafkaTopicName1",
      "additionalAttributes": {
        "type": "topic"
      }
    },
    {
      "id": "def",
      "name": "kafkaTopicName2",
      "additionalAttributes": {
        "type": "topic"
      }
    }
  ],
  "schemas": [
    {
      "id": "789",
      "hints": [],
      "schemaType": "AVRO",
      "content": "{\"type\":\"record\",\"name\":\"MQL\",\"namespace\":\"com.myenterprise.sales.kpi\",\"fields\":[{\"name\":\"kpiId\",\"type\":{\"type\":\"record\",\"name\":\"Myid\",\"fields\":[{\"name\":\"myMQLType\",\"type\":[\"null\",\"string\"]}]}},{\"name\":\"LeadId\",\"type\":[\"null\",\"string\"]},{\"name\":\"myApplicationId\",\"type\":[\"null\",\"string\"]},{\"name\":\"leadType\",\"type\":[\"null\",\"string\"]},{\"name\":\"SQL\",\"type\":{\"type\":\"record\",\"name\":\"mySQLObject\",\"fields\":[{\"name\":\"sales\",\"type\":[\"null\",\"string\"]}]}},{\"name\":\"myUserId\",\"type\":[\"null\",\"string\"]},{\"name\":\"msgVersion\",\"type\":[\"null\",\"string\"]},{\"name\":\"myClassName\",\"type\":[\"null\",\"string\"]}]}",
      "primitive": false,
      "additionalAttributes": {
        "subjectName": "MyCompany.Sales.Europe.Leads-com.myenterprise.sales.kpi.MQL",
        "subjectVersion": 1,
        "schemaReg": true
      }
    }  
  ],
  "clientToChannelRelationships": [
    {
      "clientId": "123",
      "channelId": "abc"
    },
    {
      "clientId": "123",
      "channelId": "def"
    }
  ],
  "channelToSchemaRelationships": [
    {
      "channelId": "abc",
      "schemaId": "789"
    },
    {
      "channelId": "def",
      "schemaId": "789"
    }
  ]
}
```

Example 3: Solace client binds to a queue which has topic subscriptions

```
{
  "broker": {
    "brokerType": "solace",
    "hostname": "34.23.54.65",
    "additionalAttributes": {
      "vpnName": "myVpnName"
    }
  },
  "clients": [
    {
      "id": "123",
      "type": "consumer",
      "name": "solaceClientName",
      "additionalAttributes": {
        "clientType": "clientApplication"
        "clientUsername": "myClientUsername"
      }
    }
  ],
  "channels": [
    {
      "id": "456"
      "name": "solaceQueueName",
      "additionalAttributes": {
        "type": "queue",
        "durable": true,
        "exclusive": false
      }
    }
  ],
  "subscriptions": [
    {
      "id": "789",
      "matchCriteria": "my/topic/space/>"
    },
    {
      "id": "790",
      "matchCriteria": "us/nasdaq/f*/tick"
    }
  ],
  "clientToChannelRelationships": [
    {
      "clientId": "123",
      "channelId": "456"
    }
  ],
  "channelToSubscriptionRelationships": [
    {
      "channelId": "456",
      "subscriptionId": "789"
    },
    {
      "channelId": "456",
      "subscriptionId": "790"
    }
  ]
}
```

Example 4: Rabbit MQ client binds to queue which binds to topic exchange.

```
{
  "broker": {
    "brokerType": "rabbitmq",
    "hostname": "34.23.54.65"
  },
  "clients": [
    {
      "id": "123",
      "name": "myUserId",
      "type": "consumer",
      "additionalAttributes": {
        "clientType": "applicationClient",
      }
    }
  ],
  "channels": [
    {
      "id": "456",
      "name": "myQueueName",
      "additionalAttributes": {
        "type": "queue"
      }
    },
    {
      "id": "exch",
      "name": "myExchange",
      "additionalAttributes": {
        "type": "exchange",
        "exchangeType": "topic"
      }
    }
  ],
  "subscriptions": [
    {
      "id": "789",
      "matchCriteria": "stocks.us.nasdaq.*"
    }
  ],
  "clientToChannelRelationships": [
    {
      "clientId": "123",
      "channelId": "456"
    }
  ],
  "channelToSubscriptionRelationships": [
    {
      "channelId": "456",
      "subscriptionId": "789"
    }
  ],
  "channelToChannelRelationships": [
    {
      "sourceChannelId": "456",
      "targetChannelId": "exch"
    }
  ]
}
```
