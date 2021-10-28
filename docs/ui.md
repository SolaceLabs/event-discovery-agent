# Web Interface
The discovery agent has a builtin user interface which runs on port 8120 by default:
[http://localhost:8120](http://localhost:8120)

The UI has built in forms for Apache Kafka, Confluent Kafka, Solace PubSub+ and AWS MSK scans.

## Apache Kafka

Here is an example Kafka scan form:ß

![kafka UI example](./img/KafkaScanExample.png?raw=true "Kafka Scan Form")

The Kafka form allows the user to input a truststore for authenticated transport as well as a keystore for mutual authentication. The authentication methods include no authentication, SASL_PLAIN, SASL_GSSAPI, SASL_SCRAM_256 and SASL_SCRAM_512.

Additionally, the user can specify a Kafka Connect cluster using basic authentication and TLS transport or mutual authentication.

## Solace PubSub+

Here is an example Solace PubSub+ scan form:

![Solace PubSub+ UI example](./img/SolaceScanExample.png?raw=true "Solace Scan Form")

The Solace PubSub+ form allows the user to enter client and semp (management) credentials as well as a set of topic subscriptions to use for the timed event scan. On a DMR cluster it is recommended to use `#noexport/` in front of the topic subscription.

## Confluent Kafka

The Confluent Kafka form allows the user to enter the host, port and credentials for a Confluent Schema Registry, a Kafka connect cluster as well as the admin credentials for a Kafka broker.
