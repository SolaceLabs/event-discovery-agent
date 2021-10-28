import React from 'react';
import './App.scss';
import LandingPage from './components/LandingPage/LandingPage';
import {
	HashRouter as Router,
	Route,
	Switch
} from "react-router-dom";
import PluginContainer from './components/PluginContainer/PluginContainer';


const App = () => (
	<Router>
		<div className="discovery-ui">
			<Switch>
				<Route path="/kafka-plugin" exact render={() => <PluginContainer plugin={"kafka"} />} />
				<Route path="/confluent-plugin" exact render={() => <PluginContainer plugin={"confluent"} />} />
				<Route path="/solace-plugin" exact render={() => <PluginContainer plugin={"solace"} />} />
				<Route path="/solace-runtime-plugin" exact render={() => <PluginContainer plugin={"solaceRuntime"} />} />
				<Route path="/amazon-plugin" exact render={() => <PluginContainer plugin={"amazon"} />} />
				<Route path="/hivemq-plugin" exact render={() => <PluginContainer plugin={"hiveMQRuntime"} />} />
				<Route path="/rabbitmq-plugin" exact render={() => <PluginContainer plugin={"RABBITMQRuntime"} />} />
				<Route path="/nats-plugin" exact render={() => <PluginContainer plugin={"natsRuntime"} />} />
				<Route path="/" component={LandingPage}/>
			</Switch>
		</div>
	</Router>

)

export default App;