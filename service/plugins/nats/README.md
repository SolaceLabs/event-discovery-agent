# NATS Discovery PoC

## NATS Setup

### Start a NATS server

> docker-compose up -d

### Subscribe on Subjects

The nats-server doesn't come bundled with any clients. But most client libraries come with sample programs that allow you to publish, subscribe, send requests and reply messages.

Clone the repo (https://github.com/nats-io/nats.java), then start the subscriber.

```bash
java -cp build/libs/jnats-2.12.1-SNAPSHOT.jar:build/libs/jnats-2.12.1-SNAPSHOT-examples.jar io.nats.examples.NatsSub -s nats://localhost:4222 "TEST.*" 100

Trying to connect to nats://localhost:4222, and listen to TEST.* for 100 messages.

Status change nats: connection opened
```

### Create Streams

Install the [The NATS Command Line Interface](https://github.com/nats-io/natscli) first, then follow the [official document](https://docs.nats.io/jetstream/administration/streams) to create streams.

### NATS monitoring

Check http://localhost:8222/connz?subs=1 and http://localhost:8222/jsz?streams=1&config=1

## NATS Discovery

1. Start the agent

`java -jar services/eventDiscoveryService/target/event-discovery-agent-1.0-SNAPSHOT.jar`

2. Invoke the NATS agent

```bash
curl -X POST http://localhost:8120/api/v0/event-discovery-agent/local/app/operation -H 'content-type: application/json' -d '{
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
        ]
    }
}'
{"data":{"brokerIdentity":{"brokerType":"NATS","hostname":"localhost","clientProtocol":"nats","clientPort":4222,"adminPortocol":"http","adminPort":8222},"brokerAuthentication":null,"discoveryOperation":{"operationType":"eventDiscovery","messageQueueLength":100000,"durationInSecs":10,"subscriptionSet":[">"],"name":null},"jobId":"uabqlzoe2d"}}%
```

3. During the discovery, publish messages to the NATS server

```bash
java -cp build/libs/jnats-2.12.1-SNAPSHOT.jar:build/libs/jnats-2.12.1-SNAPSHOT-examples.jar io.nats.examples.NatsPub -s nats://localhost:4222 TEST.1 "hello"

Publishing 'hello' to 'TEST.1', server is nats://localhost:4222

Status change nats: connection opened
Status change nats: connection closed

java -cp build/libs/jnats-2.12.1-SNAPSHOT.jar:build/libs/jnats-2.12.1-SNAPSHOT-examples.jar io.nats.examples.NatsPub -s nats://localhost:4222 TEST.2 "hello"

Publishing 'hello' to 'TEST.2', server is nats://localhost:4222

Status change nats: connection opened
Status change nats: connection closed
```

4. Download the discovery file

```bash
wget -O uabqlzoe2d.json http://localhost:8120/api/v0/event-discovery-agent/local/app/operation/uabqlzoe2d/result/asyncapi
--2021-10-09 16:16:07--  http://localhost:8120/api/v0/event-discovery-agent/local/app/operation/uabqlzoe2d/result/asyncapi
Resolving localhost (localhost)... ::1, 127.0.0.1
Connecting to localhost (localhost)|::1|:8120... connected.
HTTP request sent, awaiting response... 200 OK
Length: 1693 (1.7K) [application/json]
Saving to: ‘=uabqlzoe2d.json’

=uabqlzoe2d.json             100%[=============================================>]   1.65K  --.-KB/s    in 0s

2021-10-09 16:16:07 (101 MB/s) - ‘uabqlzoe2d.json’ saved [1693/1693]
```