# UI Developer's Guide

This is a step-by-step guide that will help you get your dev environment ready and start coding your UI plugin.

The UI is REACT based being bundled by webpack and hosted on `http://localhost:3000/`. After you have finished all of your code changes you will need to copy the UI artifacts to `service/services/eventDiscoveryService/src/main/resources/static` where the default UI is running and hosted on `http://localhost:8120/`

## Prerequisites:
* Node Version 14.15.0
 * If you are using macos, then the command should be
   ```
   nvm install 14.15.0
   ```
   After successfuly installing node V14.15.0, double check that this is the version you are currently using
    ```
    node --version
    ```
    Should result to the same version installed above, and if not, then run the following command
    ```
    nvm use v14.15.0
    ```
  * If you dont have nvm installed, follow these steps to install it
  ```
  git clone git://github.com/creationix/nvm.git ~/.nvm
  source ~/.nvm/nvm.sh
  ```  
* npm version 6.14.10
```
npm install -g npm@6.14.10
```
* webpack version 4.41.1
```
npm install --save-dev- webpack@4.41.1
```
* To start the UI, go to `service/services/eventDiscoveryService/src/main/resources/discovery-ui`, run `npm start` and go to `http://localhost:3000/`.
<br/>

**Now that we are ready to start coding. Let's take and example of writing a NATS plugin that adheres to the following payload which has already been developed on the backend.**

```
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

## Developing NATS Plugin:
* In `service/services/eventDiscoveryService/src/main/resources/discovery-ui/src/assets/plugins` edit `plugins.js` to add a new plugin option in `const PLUGINS`
```
{
    key: "natsRuntime",
    title: "NATS",
    description: "Runtime Discovery",
    route: "/nats-plugin"
}
```

* Add a new plugin default in `const PLUGIN_DEFAULTS`
```
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
```
* In `service/services/eventDiscoveryService/src/main/resources/discovery-ui/src/components/LandingPage` looking at `LandingPage.js` which renders all the different cards from `const PLUGINS` that we defined in the previous step.
  * In `LandingPage.js` - If you would like to add a logo on the card with specific scss, then
  ```
  "NATS": {
        logo: natsLogo,
        class: "nats-logo"
    }
  ```
  **Note: natsLogo must be imported at the top of file to where its located**
  ```import natsLogo from '../../assets/img/nats-log-black.png'```
  * The scss class should be added to `service/services/eventDiscoveryService/src/main/resources/discovery-ui/src/components/BrokerCard/BrokerCard.scss` under `.broker-logo` class
  ```
  .broker-logo {
      ....
      &.nats-logo {
          padding-left: 30px;
          height: 60px;
          padding-bottom: 0px;
      }
   ```
**Note: The key `"NATS"` must match the title `NATS` in plugins.js**

**Now that we have created the plugin card and all defaults, lets start by creating the plugin core code**

### NATS Plugin Core Code:
* Create a folder under `service/services/eventDiscoveryService/src/main/resources/discovery-ui/src/components/PluginContainer/Plugins` call it `NATSPlugin`
* Create two files under the created folder above, for example
```
service/services/eventDiscoveryService/src/main/resources/discovery-ui/src/components/PluginContainer/Plugins/NATSPlugin/NATSPlugin.js
```
AND
```
 service/services/eventDiscoveryService/src/main/resources/discovery-ui/src/components/PluginContainer/Plugins/NATSPlugin/NATSPlugin.scss
```
**You can leave `NATSPlugin.scss` empty for now unless you need to do any scss modifications**
* In general the javascript file should have the following skeleton

```
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import './NATSPlugin.scss';
import 'react-dropdown/style.css';
import logo from '../../../../assets/svg/discovery.svg';
import '../../../Header/Header.scss';
import PLUGINS from '../../../../../src/assets/plugins/plugins';
import SolaceButton from '../../../../components/SolaceButton/SolaceButton';
import backArrow from '../../../../assets/svg/back-arrow.svg';

const NATSPlugin = (props) => {
	const [scanAll, setScanAll] = useState(false);

	const { register, handleSubmit, formState, reset, getValues } = useForm({ mode: 'onChange' });

    // function to scann all topics (second radio button on the UI)
	const scanAllTopics = () => {
		if (!scanAll) {
			document.getElementById("specific-scan").checked = true;
			const data = getValues({ nested: true })
			reset({ ...data, "discoveryOperation[subscriptionSet]": "" })
			setScanAll(true);
		} else {
			setScanAll(false);
		}
	}

    // function to get specific topics to scan instead of all (first radio button on the UI)
	const scanSpecificTopics = () => {
		if (scanAll) {
			document.getElementById("scan-all-topics").checked = true;
			const data = getValues({ nested: true })
			reset({ ...data, "discoveryOperation[subscriptionSet]": undefined })
			setScanAll(false);
		} else {
			setScanAll(true);
		}
	}

    // when the user hits the start scan button all the data comes here
	const onSubmit = (data) => {
		props.onSubmit(data);
	}

    // This is just rendering the header of the page "NATS PLUGIN Runtime Discovery"
	const renderHeader = () => {
		const plugin = PLUGINS.find(plugin => plugin.key === props.plugin);
		return 	(
			<div className = "header">
				<div className="content">
					<div className="title">
						<Link to= "/"><img src={backArrow} className="back-arrow"/></Link>
						<span className="border-line"></span>
						<img src={logo}
								alt="discovery-icon"
								className="discovery-icon">
						</img>
						{plugin.title} Runtime Discovery
					</div>
				</div>
			</div>
		)
	}
		return (
		<>
		{renderHeader()}
		<div className="flex-center">
			<div className="plugin">
				<form className="discovery-form"
					onSubmit={handleSubmit(onSubmit)}>
					<div className="flex mt20">
						<label className="input-label">Discovery Name</label>
						<input name="discoveryOperation[name]"
							type="text"
							ref={register({required: true})}
							className="text-input-box"
							data-lpignore="true">
						</input>
					</div>
                    <div className="title mt20">Event Scan Details</div>
					<div className="flex mt20">
						<label className="input-label align-start">Topics Subscriptions</label>
						<div>
							<div className="flex-column">
								<div className="flex mb5 check-box">
									<input
										type="checkbox"
										className="mr10"
										id="specific-scan"
										name="specific-scan"
										checked={!scanAll}
										onChange={scanSpecificTopics}>
									</input>
									<label htmlFor="specific-scan"></label>
									<div className="flex-colum">
										<div className="topic-subscription">Scan for specific topic(s)</div>
										<div className="topic-hint">Use a line break to separate topic subscription</div>
									</div>
								</div>
								<textarea
									disabled={scanAll}
									name="discoveryOperation[subscriptionSet]"
									ref={register({ required: !scanAll })}
									className="text-area text-height with-bulletin"></textarea>
							</div>
							<div className="flex-column mt20">
							<div className="flex mb5 check-box">
									<input
										type="checkbox"
										className="mr10"
										id="scan-all-topics"
										name="scan-all-topics"
										checked={scanAll}
										onChange={scanAllTopics}>
									</input>
									<label htmlFor="scan-all-topics"></label>
									<div className="input-label">Scan for all topics</div>
								</div>
							</div>
						</div>
					</div>
						<div className="form-footer">
						<SolaceButton
							style={{ marginRight: "8px" }}
							kind="call-to-action"
							title="Start Scan"
							disabled={!formState.isValid}>
								<input type="submit"></input>
						</SolaceButton>
					</div>
				</form>
			</div>
		</div>
		</>
	)
}

export default NATSPlugin;
```

* For our specific NATSPlugin, here is what the code should look like

```
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import './NATSPlugin.scss';
import 'react-dropdown/style.css';
import logo from '../../../../assets/svg/discovery.svg';
import '../../../Header/Header.scss';
import PLUGINS from '../../../../../src/assets/plugins/plugins';
import SolaceButton from '../../../../components/SolaceButton/SolaceButton';
import backArrow from '../../../../assets/svg/back-arrow.svg';

const NATSPlugin = (props) => {

	const [scanAll, setScanAll] = useState(false);

	const { register, handleSubmit, formState, reset, getValues } = useForm({ mode: 'onChange' });

	const scanAllTopics = () => {
		if (!scanAll) {
			document.getElementById("specific-scan").checked = true;
			const data = getValues({ nested: true })
			reset({ ...data, "discoveryOperation[subscriptionSet]": "" })
			setScanAll(true);
		} else {
			setScanAll(false);
		}
	}

	const scanSpecificTopics = () => {
		if (scanAll) {
			document.getElementById("scan-all-topics").checked = true;
			const data = getValues({ nested: true })
			reset({ ...data, "discoveryOperation[subscriptionSet]": undefined })
			setScanAll(false);
		} else {
			setScanAll(true);
		}
	}

	const onSubmit = (data) => {
		props.onSubmit(data);
	}

	const renderHeader = () => {
		const plugin = PLUGINS.find(plugin => plugin.key === props.plugin);
		return 	(
			<div className = "header">
				<div className="content">
					<div className="title">
						<Link to= "/"><img src={backArrow} className="back-arrow"/></Link>
						<span className="border-line"></span>
						<img src={logo}
								alt="discovery-icon"
								className="discovery-icon">
						</img>
						{plugin.title} Runtime Discovery
					</div>
				</div>
			</div>
		)
	}

	return (
		<>
		{renderHeader()}
		<div className="flex-center">
			<div className="plugin">
				<form className="discovery-form"
					onSubmit={handleSubmit(onSubmit)}>
					<div className="flex mt20">
						<label className="input-label">Discovery Name</label>
						<input name="discoveryOperation[name]"
							type="text"
							ref={register({required: true})}
							className="text-input-box"
							data-lpignore="true">
						</input>
					</div>
                    <div className="flex mt20">
						<label className="input-label">Hostname</label>
						<input name="brokerIdentity[hostname]"
							type="text"
							ref={register({required: true})}
							className="text-input-box"
							data-lpignore="true">
						</input>
					</div>
                    <div className="flex mt20">
                        <label className="input-label">Client Port</label>
                        <input name="brokerIdentity[clientPort]"
                            type="number"
                            ref={register({ required: true })}
                            className="text-input-box short"
                            data-lpignore="true">
                        </input>
                    </div>
                    <div className="flex mt20">
						<label className="input-label">Client Protocol</label>
						<input name="brokerIdentity[clientProtocol]"
							type="text"
							ref={register({required: true})}
							className="text-input-box"
							data-lpignore="true">
						</input>
					</div>
                    <div className="flex mt20">
						<label className="input-label">Admin Protocol</label>
						<input name="brokerIdentity[adminProtocol]"
							type="text"
							ref={register({required: true})}
							className="text-input-box"
							data-lpignore="true">
						</input>
					</div>
                    <div className="flex mt20">
                        <label className="input-label">Admin Port</label>
                        <input name="brokerIdentity[adminPort]"
                            type="number"
                            ref={register({ required: true })}
                            className="text-input-box short"
                            data-lpignore="true">
                        </input>
                    </div>
                    <div className="title mt20">Event Scan Details</div>
					<div className="flex mt20">
						<label className="input-label align-start">Topics Subscriptions</label>
						<div>
							<div className="flex-column">
								<div className="flex mb5 check-box">
									<input
										type="checkbox"
										className="mr10"
										id="specific-scan"
										name="specific-scan"
										checked={!scanAll}
										onChange={scanSpecificTopics}>
									</input>
									<label htmlFor="specific-scan"></label>
									<div className="flex-colum">
										<div className="topic-subscription">Scan for specific topic(s)</div>
										<div className="topic-hint">Use a line break to separate topic subscription</div>
									</div>
								</div>
								<textarea
									disabled={scanAll}
									name="discoveryOperation[subscriptionSet]"
									ref={register({ required: !scanAll })}
									className="text-area text-height with-bulletin"></textarea>
							</div>
							<div className="flex-column mt20">
							<div className="flex mb5 check-box">
									<input
										type="checkbox"
										className="mr10"
										id="scan-all-topics"
										name="scan-all-topics"
										checked={scanAll}
										onChange={scanAllTopics}>
									</input>
									<label htmlFor="scan-all-topics"></label>
									<div className="input-label">Scan for all topics</div>
								</div>
							</div>
						</div>
					</div>
						<div className="form-footer">
						<SolaceButton
							style={{ marginRight: "8px" }}
							kind="call-to-action"
							title="Start Scan"
							disabled={!formState.isValid}>
								<input type="submit"></input>
						</SolaceButton>
					</div>
				</form>
			</div>
		</div>
		</>
	)
}
export default NATSPlugin;
```
* In `service/services/eventDiscoveryService/src/main/resources/discovery-ui/src/components/PluginContainer/PluginContainer.js` we would need to import our new plugin and make sure on a card click we navigate to the right view.
```
import NATSPlugin from './Plugins/NATSPlugin/NATSPlugin';
```
```
{props.plugin === 'natsRuntime' && <NATSPlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
```
**Now that we have created the plugin core code, the last step needed is to create a route to the plugin `natsRuntime`**

* In `service/services/eventDiscoveryService/src/main/resources/discovery-ui/src` edit `App.js` and add
`<Route path="/nats-plugin" exact render={() => <PluginContainer plugin={"natsRuntime"} />} />` - note that `natsRuntime` must match the plugin key defined in `plugins.js`

**After finishing developing your plugin on the UI, it will still only be hosted on `http://localhost:3000/` in order to get it in the static directory and to see your changes on `http://localhost:8120/`, you have two options here**
* Option one; go to `service/services/eventDiscoveryService/src/main/resources/discovery-ui/` and run `npm run build`
* Option two; go to `service/` and run `mvn clean install`. This will likely take around 3-4 minutes, so if you only want to move your UI changes, we suggest doing `Option one`
