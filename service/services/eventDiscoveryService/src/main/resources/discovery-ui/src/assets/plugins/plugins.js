const PLUGINS = [
	{
		key: "solaceRuntime",
		title: "Solace PubSub+",
		description: "Runtime Discovery",
		route: "/solace-runtime-plugin"
	},
	{
		key: "kafka",
		title: "Apache Kafka",
		description: "Runtime Discovery",
		route: "/kafka-plugin"
	},
	{
		key: "confluent",
		title: "Confluent",
		description: "Runtime Discovery",
		route: "/confluent-plugin"
	},
	{
		key: "amazon",
		title: "Amazon MSK",
		description: "Runtime Discovery",
		route: "/amazon-plugin"
	},
	{
		key: "hiveMQRuntime",
		title: "HiveMQ",
		description: "Runtime Discovery",
		route: "/hivemq-plugin"
	},
	{
		key: "RABBITMQRuntime",
		title: "RABBITMQ",
		description: "Runtime Discovery",
		route: "/rabbitmq-plugin"
    },
	{
		key: "natsRuntime",
		title: "NATS",
		description: "Runtime Discovery",
		route: "/nats-plugin"
	}
];

export const PLUGIN_DEFAULTS = {
	solace: {
		brokerIdentity: {
			brokerType: "SOLACE"
		},
		brokerAuthentication: {
			brokerType: "SOLACE"
		},
		discoveryOperation: {
			operationType: "topicAnalysis",
			durationInSecs: 10,
			subscriptionSet: []
		}
	},
	solaceRuntime: {
		brokerIdentity: {
			brokerType: "SOLACE"
		},
		brokerAuthentication: {
			brokerType: "SOLACE"
		},
		discoveryOperation: {
			operationType: "eventDiscovery",
			messageQueueLength: 100000,
			durationInSecs: 10,
			subscriptionSet: []
		}
	},
	kafka: {
		brokerIdentity: {
			brokerType: "KAFKA"
		},
		brokerAuthentication: {
			brokerType: "KAFKA",
			authenticationType: "NOAUTH",
			transportType: "PLAINTEXT"
		},
		discoveryOperation: {
			name: "",
			operationType: "eventDiscovery",
			messageQueueLength: 100000,
			durationInSecs: 10,
			subscriptionSet: []
		}
	},
	confluent: {
		brokerIdentity: {
			brokerType: "CONFLUENT"
		},
		brokerAuthentication: {
			brokerType: "CONFLUENT",
			authenticationType: "NOAUTH",
			transportType: "PLAINTEXT"

		},
		discoveryOperation: {
			name: "",
			operationType: "eventDiscovery",
			messageQueueLength: 100000,
			durationInSecs: 3,
			subscriptionSet: []
		}
	},
	hiveMQRuntime: {
		brokerIdentity: {
			brokerType: "HIVEMQ"
		},
		discoveryOperation: {
			name: "",
			operationType: "eventDiscovery",
			messageQueueLength: 100000,
			durationInSecs: 10,
			subscriptionSet: ['#']
		}
	},
	RABBITMQRuntime: {
		brokerIdentity: {
			brokerType: "RABBITMQ"
		},
		discoveryOperation: {
			name: "",
			operationType: "eventDiscovery",
			messageQueueLength: 100000,
			durationInSecs: 10,
			subscriptionSet: ['#']
    }
  },
	natsRuntime: {
		brokerIdentity: {
			brokerType: "NATS"
		},
		discoveryOperation: {
			name: "nats scan",
			operationType: "eventDiscovery",
			messageQueueLength: 100000,
			durationInSecs: 10,
			subscriptionSet: ['>']
		}
	}
}
export default PLUGINS;