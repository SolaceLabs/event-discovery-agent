![Image of telescope](./docs/img/discovery.png)

# Event Discovery Agent

Discover event streams flowing through a broker.

The Discovery Agent can discover event broker configuration, scan a schema registry and optionally attach a passive listener to a broker to scan events as they flow and then present the discovered data in AsyncAPI format. The discovery agent is built on a plugin architecture that allows it to be easily extended for additional broker types.

## Broker Plugins

The discovery agent comes with the following event or message broker plugins included:

* **Apache Kafka** - support for various authentication and transport schemes (SASL_PLAIN, SASL_GSSAPI, SASL_SCRAM, SSL transport and SSL Mutual authentication). This plugin first gathers some broker configuration and then pulls the last record from each topic and reverse engineers a schema. This plugin has also been tested agains Apache Kafka variants such as Amazon MSK and Redpanda. *well tested*
* **Confluent** - support for various authentication and transport schemes (SASL_PLAIN, SASL_GSSAPI, SASL_SCRAM, SSL transport and SSL Mutual authentication). This plugin first gathers some broker configuration and then scans Confluent Schema Registry to determine the schemas used for each topic. *well tested*
* **Solace PubSub+** - support for basic authentication and truststore credentials. This plugin uses the SEMPv2 admin API to gather broker configuration, and then runs a timed event scan on the broker to discover dynamic topics and reverse engineer schemas. This plugin has been tested with various versions of PubSub+ broker from 9.2 to 9.8. *well tested*
* **NATS** - supports username/password credentials. This plugin gathers client data from the broker and then runs a timed event scan on the broker to discover dynamic topics and reverse engineer schemas. *untested*
* **RabbitMQ** - supports username/password credentials. This plugin gathers client data from the broker and then runs a timed event scan on the broker to discover dynamic topics and reverse engineer schemas. *untested*
* **HiveMQ** - supports brokers with no authentication. This plugin gathers client data from the broker and then runs a timed event scan on the broker to discover dynamic topics and reverse engineer schemas. *untested*

## Running the Discovery Agent

### Prerequisites:
* Java 11
* Maven 3.2+

### Cloning and Building
```
git clone https://github.com/SolaceLabs/event-discovery-agent.git
cd event-discovery-agent/service
mvn clean install
```

### Running the Discovery Agent
```
java -jar services/eventDiscoveryService/target/event-discovery-agent-app-1.0-SNAPSHOT.jar
```

## Running a scan

### Web Interface
The discovery agent has a built-in user interface which runs on port 8120 by default:
[http://localhost:8120](http://localhost:8120)

See [UI Documentation](docs/ui.md) for additional information

### REST interface

The Discovery Agent includes a set of REST APIs that allow the user to initiate a scan, get the scan state and retrieve the scan result. Each plugin requires it own custom set of authentication and identification attributes that must be supplied by the user.

See [REST Documentation](docs/rest.md) for additional information

## Contributions

Contributions are encouraged! If you have ideas to improve an existing plugin, create a new plugin, or improve/extend the agent framework then please contribute!

For more information on the internal details of plugins, see [developer's plugin guide](./docs/plugindevelopersguide.md)

For more information on developing your own plugin UI, see [developer's UI guide](./docs/uidevelopersguide.md)

## Motivations

See [motivations](./docs/motivations.md)

## Limitations

See [limitations](./docs/limitations.md)
