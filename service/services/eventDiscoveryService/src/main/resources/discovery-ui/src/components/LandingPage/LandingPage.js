import React, { useState, useEffect } from "react";
import './LandingPage.scss';
import Header from '../Header/Header'
import BrokerCard from '../BrokerCard/BrokerCard';
import PLUGINS from '../../assets/plugins/plugins';
import solaceLogo from '../../assets/img/solace-log-black.png'
import kafkaLogo from '../../assets/svg/kafka_logo.svg';
import genericBrokerLogo from '../../assets/svg/generic_broker_logo.svg';
import genericCloudLogo from '../../assets/svg/generic_cloud_logo.svg';


const brokerLogos = {
	"Apache Kafka": {
		logo: kafkaLogo,
		class: "kafka-logo"
	},
	"Confluent": {
		logo: genericBrokerLogo,
		class: "kafka-logo"
	},
	"Amazon MSK": {
		logo: genericCloudLogo,
		class: "kafka-logo"
	},
	"Solace PubSub+": {
		logo: solaceLogo,
		class: "solace-logo"
	},
	"HiveMQ":{
		logo: genericBrokerLogo,
		class: "generic-logo"
	},
	"RABBITMQ": {
		logo: genericBrokerLogo,
    class: "generic-logo"
  },
	"NATS": {
		logo: genericBrokerLogo,
		class: "generic-logo"
	}
}


const LandingPage = () => {
	const getCards = () => {
		return PLUGINS.map((plugin) => {
			return (
				<BrokerCard
					key={plugin.key}
					logo={brokerLogos[plugin.title].logo}
					logoClass={brokerLogos[plugin.title].class}
					route={plugin.route}
					title={plugin.title}
					description={plugin.description}/>
			);
		})
	}

	return (
		<div className="width100">
			<Header />
			<div className="flex-center">
				<div className="landing-page">
					<div className="landing-page-body">
						<div className="title">
							Select the broker type to perform a run-time discovery scan
						</div>
						{getCards()}
					</div>
				</div>
			</div>
		</div>
	)
}

export default LandingPage;