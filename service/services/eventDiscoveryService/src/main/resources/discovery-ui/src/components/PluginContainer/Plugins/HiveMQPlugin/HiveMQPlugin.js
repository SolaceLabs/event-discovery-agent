import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import './HiveMQPlugin.scss';
import 'react-dropdown/style.css';
import logo from '../../../../assets/svg/discovery.svg';
import '../../../Header/Header.scss';
import PLUGINS from '../../../../../src/assets/plugins/plugins';
import SolaceButton from '../../../../components/SolaceButton/SolaceButton';
import backArrow from '../../../../assets/svg/back-arrow.svg';

const HiveMQPlugin = (props) => {
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
							ref={register({required: true})}
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
						<label className="input-label">Admin Port</label>
						<input name="brokerIdentity[adminPort]"
							type="number"
							ref={register({required: true})}
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

export default HiveMQPlugin;
