# REST interface

The Discovery Agent includes a set of REST APIs that allow the user to initiate a scan, get the scan state and retrieve the scan result.

Each plugin requires its own custom set of authentication and identification attributes that must be supplied by the user.

## How to retrieve scan results

Broker scans may be lengthy and are therefore handled asynchronously by the discovery agent. When submitting a scan request, the response includes a jobId that must be used to retrieve the result.

Here is an example response when initiating a scan:

```
{
    "data": {
        "brokerIdentity": {
            "brokerType": "RABBITMQ",
            "hostname": "127.0.0.1",
               ...
        },
        "brokerAuthentication": null,
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 1,
            "subscriptionSet": [
                "#"
            ],
            "name": "rabbit scan"
        },
        "jobId": "1u292mafbt"
    }
}
```

In this instance, the jobId is `1u292mafbt`. To retrieve the result for this scan, use the following REST APIs

```
GET http://localhost:8120/api/v0/event-discovery-agent/local/app/operation/1u292mafbt/result/asyncapi
```

where the jobId is in the REST URL.

## Initiating a scan request

In all cases, a scan is initiated with a POST to a fixed URL:

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation
```
with a header of `Content-Type: application/json`

The body of the POST varies depending on the broker type, and authentication types. The following sections document the body types.

## Apache Kafka

The Apache Kafka scans require configured users with administrative and client messaging permissions. These users are specified individually, so they could be different users or the same user used in both instances.

The Apache Kafka plugin does not do a timed message scan, so the `durationInSecs` field in the `discoveryOperation` object is not applicable.

The `subscriptionSet` list, is the list of topics to be included in the scan. If it is empty, then all topics are included. ** Regex is not supported **

Here are some example Kafka requests.

### No authentication

The simplest request:

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

### SASL_PLAIN

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "discovery.kafka.net:9093"
        },
        "brokerAuthentication": {
        	"brokerType": "KAFKA",
        	"authenticationType": "SASL_PLAIN",
        	"transportType": "PLAINTEXT",
        	"adminUsername": "myuser",
        	"adminPassword": "mypass",
        	"consumerUsername": "myuser",
        	"consumerPassword": "mypass",
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

### SASL_GSS

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "discovery.mybroker.net:19094"
        },
        "brokerAuthentication": {
        	"brokerType": "KAFKA",
        	"authenticationType": "SASL_GSSAPI",
        	"transportType": "PLAINTEXT",
          "serviceName": "myservice",
        	"principal": "kafka_consumer@TEST.MYDOMAIN.NET",
        	"keyTabLocation": "/path/to/my/saslconsumer.keytab",
        	"krb5ConfigurationLocation": "/path/to/my/krb.conf"
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

### SASL_SCRAM

SASL_SCRAM_512 is also supported.

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "kafka1:14091",
        },
        "brokerAuthentication": {
        	"brokerType": "KAFKA",
        	"authenticationType": "SASL_SCRAM_256",
        	"adminUsername": "admin",
        	"adminPassword": "admin-secret-256",
        	"consumerUsername": "admin",
        	"consumerPassword": "admin-secret-256",
        	"transportType": "PLAINTEXT"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 8,
            "subscriptionSet": [
            ],
            "name": "kafka scan"
        }
}

```

### SASL_PLAIN with TLS Transport

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "kafka.local:19092"
        },
        "brokerAuthentication": {
        	"brokerType": "KAFKA",
        	"authenticationType": "SASL_PLAIN",
        	"transportType": "SSL",
        	"adminUsername": "myuser",
        	"adminPassword": "mypass",
        	"consumerUsername": "myuser",
        	"consumerPassword": "mypass",
        	"trustStoreLocation": "/pathTo/keyFiles/client.ts",
        	"trustStorePassword": "trustpass123"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 8,
            "subscriptionSet": [  
            ],
            "name": "kafka scan"
        }
}
```

### SASL_PLAIN with TLS mutual authentication

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "kafka.local:19092"
        },
        "brokerAuthentication": {
        	"brokerType": "KAFKA",
        	"authenticationType": "SASL_PLAIN",
        	"transportType": "SSL",
        	"adminUsername": "myuser",
        	"adminPassword": "mypass",
        	"consumerUsername": "myuser",
        	"consumerPassword": "mypass",
        	"trustStoreLocation": "/pathTo/keyFiles/client.ts",
        	"trustStorePassword": "trustpass123",
        	"keyStoreLocation": "/pathTo/client.ks",
        	"keyStorePassword": "storepass123",
        	"keyPassword": "keypass123"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 8,
            "subscriptionSet": [  
            ],
            "name": "kafka scan"
        }
}
```

### Kafka connector cluster with basic auth + TLS transport

In this case the Kafka broker is unauthenticated but the connect cluster uses basic auth with TLS transport.

```
{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "kafka1:12091",
          "connector": {
              "hostname": "connect",
              "port": 8083
          }
        },
        "brokerAuthentication": {
        	"brokerType": "CONFLUENT",
        	"authenticationType": "NOAUTH",
        	"transportType": "PLAINTEXT",
        	"connector": {
	        	"basicAuthUsername": "superUser",
	        	"basicAuthPassword": "superUser",
	        	"trustStoreLocation": "/path/to/kafka.connect.truststore.jks",
	        	"trustStorePassword": "confluent"
        	}

        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 8,
            "subscriptionSet": [

            ],
            "name": "Kafka"
        }
}
```

### SASL_GSS with Mutual authentication and Connector (with no auth)

This example uses SASL_GSS with TLS mutual authentication and a connector with no authentication:

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "KAFKA",
          "host": "broker.kerberos-demo.local:9092",
          "connector": {
              "hostname": "connect.kerberos-demo.local",
              "port": 8083
          }
        },
        "brokerAuthentication": {
        	"brokerType": "KAFKA",
        	"authenticationType": "SASL_GSSAPI",
        	"principal": "kafka_consumer@TEST.MYDOMAIN.NET",
        	"keyTabLocation": "/path/to/my/gssapi_ssl/kafka-client.key",
        	"krb5ConfigurationLocation": "/path/to/my/gssapi_ssl/krb5.conf",
          "serviceName": "kafka",
          "transportType": "SSL",
        	"trustStoreLocation": "/path/to/my/Downloads/gssapi_ssl/kafka.connect.truststore.jks",
        	"trustStorePassword": "mypass",
        	"keyStoreLocation": "/path/to/my/gssapi_ssl/kafka.connect.keystore.jks",
        	"keyStorePassword": "mypass",
        	"keyPassword": "mypass"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 8,
            "subscriptionSet": [  
            ],
            "name": "my special kerberos scan"
        }
}
```

## Solace PubSub+

The PubSub+ plugin uses the SEMP API to collect configured and dynamic broker data such as information on queues, clients and subscriptions. After this, a timed message scan is started (using SMF) that subscribes to a set of topics described in the `subscriptionSet` field and it runs for the duration detailed in the `durationInSecs` field. The agent framework uses the [reactor pattern](https://www.youtube.com/watch?v=5ilYW2VqTqQ) to offload processing from the messaging threads. The length of the reactor queue is defined by the `messageQueueLength` attribute.

### Username/Password authentication

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
            "brokerType": "SOLACE",
          	"clientProtocol": "tcps",
          	"sempHost": "pubsubplus.local.net",
          	"messagingPort": "55443",
          	"messageVpn": "myMsgVpn",
          	"sempPort": "8080",
            "sempScheme": "http",
            "clientHost": "pubsubplus.local.net",
            "brokerAlias": "daily-dev"

        },
        "brokerAuthentication": {
            "brokerType": "SOLACE",
          	"clientUsername": "messaging",
          	"clientPassword": "mypass1",
          	"sempUsername": "admin",
          	"sempPassword": "mypass2"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 10,
            "subscriptionSet": [
                "#noexport/>"
            ],
            "name": "SolaceScan"
        }
}
```

### Certificate authentication

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
            "brokerType": "SOLACE",
            "clientProtocol": "tcps",
            "clientHost": "pubsub.local.net",
            "sempHost": "pubsub.local.net",
            "messagingPort": "55443",
            "messageVpn": "default",
            "sempPort": "1943",
            "sempScheme": "https",
            "brokerAlias": "myBroker"
        },
        "brokerAuthentication": {
            "brokerType": "SOLACE",
            "sempUsername": "admin",
            "sempPassword": "adminPass",
            "trustStoreLocation": "/path/to/myTrustStore.jks",
            "trustStorePassword": "mypass1",
            "keyStoreLocation": "/path/to/keystore.jks",
            "keyStorePassword": "mypass2",
            "keyPassword": "mypass3"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 10,
            "subscriptionSet": [
                "#noexport/>", "#P2P/>"
            ],
            "name": "solace scan"
        }
}
```

## Confluent KAFKA

The Kafka Confluent plugin first uses the kafka broker admin APIs to learn about topics, consumer groups, etc... then connects to the schema registry and pulls the last schema from each topic, and depending on the [Subject Name Strategy](https://docs.confluent.io/platform/current/schema-registry/serdes-develop/index.html#sr-schemas-subject-name-strategy), attempts to determine the associated topic.

This plugin does not run a live messaging scan so the `durationInSecs` attribute can be ignored.

The user must specify broker authentication parameters for the admin API on the broker, as well as authentication parameters for the Confluent schema registry (and optionally a Kafka Connect cluster).

### Kafka no auth, Schema Registry no auth (and a connector with no auth)

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
          "brokerType": "CONFLUENT",
          "host": "confluent.kafka.net:9092",
          "schemaRegistry": {
              "hostname": "confluent.kafka.net",
              "port": 8081
          },
          "connector": {
              "hostname": "confluent.kafka.net",
              "port": 18083
          }
        },
        "brokerAuthentication": {
        	"brokerType": "CONFLUENT",
        	"authenticationType": "NOAUTH",
        	"transportType": "PLAINTEXT"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 8,
            "subscriptionSet": [
            ],
            "name": "Confluent scan"
        }
}
```

### Kafka no auth, Schema Registry basic auth + TLS transport (and Connector cluster)

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
            "brokerType": "CONFLUENT",
            "host": "broker.kafka.net:12091",
            "schemaRegistry": {
                "hostname": "schemareg.kafka.net",
                "port": 8085
            },
            "connector": {
                "hostname": "connector.kafka.net",
                "port": 8083
            }
        },
        "brokerAuthentication": {
        	"brokerType": "CONFLUENT",
        	"authenticationType": "NOAUTH",
        	"transportType": "PLAINTEXT",
        	"schemaRegistry": {
	        	"basicAuthUsername": "schemaRegUsername",
	        	"basicAuthPassword": "schemaRegPassword",
	        	"trustStoreLocation": "/path/to/kafka.schemaregistry.truststore.jks",
	        	"trustStorePassword": "tspass1"
        	},
        	"connector": {
	        	"basicAuthUsername": "superUser",
	        	"basicAuthPassword": "superUser",
	        	"trustStoreLocation": "/path/to/kafka.connect.truststore.jks",
	        	"trustStorePassword": "tspass2"
        	}

        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 8,
            "subscriptionSet": [

            ],
            "name": "ConfluentScan"
        }
}
```

## Nats

Connects to the admin API to collect client information, then connects as a messaging client to actively listen for events and reverse engineer schemas (one per topic).

### No auth

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
    "brokerIdentity": {
        "brokerType": "NATS",
        "hostname": "localhost",
        "clientPort": "4222",
        "clientProtocol": "nats",
        "adminProtocol": "http",
        "adminPort": "8222"
    },
    "discoveryOperation": {
        "operationType": "eventDiscovery",
        "messageQueueLength": 100000,
        "durationInSecs": 10,
        "subscriptionSet": [
            ">"
        ],

        "name": "nats scan"
    }
}
```

## RabbitMQ

Connects to the admin API to collect client information, then connects as a messaging client to actively listen for events and reverse engineer schemas (one per topic).

### Username/Password auth

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
            "brokerType": "RABBITMQ",
          	"hostname": "127.0.0.1",
          	"clientPort": "5672",
          	"clientProtocol": "tcp",
          	"adminPort": "15672",
            "clientUsername": "guest",
            "clientPassword": "guest"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 1,
            "subscriptionSet": [
                "#"
            ],
            "name": "rabbit scan"
        }
}
```

## HiveMQ

Connects to the admin API to collect client information, then connects as a messaging client to actively listen for events and reverse engineer schemas (one per topic).

### No auth

```
POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation

{
        "brokerIdentity": {
            "brokerType": "HIVEMQ",
            	"hostname": "localhost",
            	"clientPort": "1883",
            	"clientProtocol": "tcp",
            	"adminPort": "8888"
        },
        "discoveryOperation": {
            "operationType": "eventDiscovery",
            "messageQueueLength": 100000,
            "durationInSecs": 10,
            "subscriptionSet": [
                "#"
            ],
            "name": "My HiveMQ Scan"
        }
}
```
