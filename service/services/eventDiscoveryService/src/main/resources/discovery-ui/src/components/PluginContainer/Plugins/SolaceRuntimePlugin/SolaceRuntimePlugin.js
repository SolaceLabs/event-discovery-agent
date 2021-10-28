import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import './SolaceRuntimePlugin.scss';
import Dropdown from 'react-dropdown';
import CheckBox from '../../../CheckBox/CheckBox';
import 'react-dropdown/style.css';
import logo from '../../../../assets/svg/discovery.svg';
import '../../../Header/Header.scss';
import PLUGINS from '../../../../../src/assets/plugins/plugins';
import backArrow from '../../../../assets/svg/back-arrow.svg';
import SolaceButton from '../../../../components/SolaceButton/SolaceButton';

const SolaceRuntimePlugin = (props) => {

	const sempSchemes = [
		{ value: 'http', label: 'Http' },
		{ value: 'https', label: 'Https' }
	];

	const [sempSchemeType, setSempSchemeType] = useState(sempSchemes[0]);
	const [hasCustomTrustStore, setHasCustomTrustStore] = useState(false);
	const [hasKeyStore, setHasKeyStore] = useState(false);
	const [hasBasicAuth, setBasicAuth] = useState(true);

	const { register, handleSubmit, formState, reset, getValues, setValue } = useForm({ mode: 'onChange' });

	const onSubmit = (data) => {
		data.brokerIdentity.sempScheme = sempSchemeType.value;
		props.onSubmit(data);
	}

	const toggleCustomTrustStore = () => {
		setHasCustomTrustStore(!hasCustomTrustStore);
	}

	const toggleKeyStore = () => {
		setHasKeyStore(!hasKeyStore);
	}

	const toggleBasicAuth = () => {
		setBasicAuth(!hasBasicAuth);
	}

	const focus = (e) => {
		document.getElementById(e.currentTarget.id).focus();
	}

	const renderHeader = () => {
		const plugin = PLUGINS.find(plugin => plugin.key === 'solaceRuntime');
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
				<div className="title mt20">Target Event Broker Details</div>
				<div className="description">Solace PubSub+ Runtime Discovery is used to scan your PubSub+ brokers, discover the event stream related data, so that it can be uploaded and imported to the Event Portal.</div>
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
					<div className="mt20">
						<div className="pl200">
							<CheckBox checked={hasBasicAuth} onChange={toggleBasicAuth} title={'Add Basic Authentication'}></CheckBox>
						</div>
						{hasBasicAuth &&
						<div>
							<div className="flex mt20">
								<label className="input-label">Client Username</label>
									<input name="brokerAuthentication[clientUsername]"
										type="text"
										ref={register({required: true})}
										className="text-input-box"
										data-lpignore="true"
										placeholder="e.g. solace-client-username">
									</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">Client Password</label>
									<input name="brokerAuthentication[clientPassword]"
										type="password"
										ref={register({required: true})}
										className="text-input-box"
										data-lpignore="true"
										placeholder="The client password, if any">
								</input>
							</div>
						</div>
					}
					</div>
					<div className="flex mt20">
						<label className="input-label">Semp Username</label>
							<input name="brokerAuthentication[sempUsername]"
								type="text-input-box"
								ref={register({required: true})}
								className="text-input-box"
								data-lpignore="true"
								placeholder="Semp username">
							</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Semp Password</label>
							<input name="brokerAuthentication[sempPassword]"
								type="password"
								ref={register({required: true})}
								className="text-input-box"
								data-lpignore="true"
								placeholder="The semp password, if any">
							</input>
					</div>
					<div className="mt20">
						<div className="pl200">
							<CheckBox checked={hasCustomTrustStore} onChange={toggleCustomTrustStore} title={'Add Custom TrustStore'}></CheckBox>
						</div>
						{hasCustomTrustStore &&
							<div>
								<div className="flex mt20">
									<label className="input-label">TrustStore Location</label>
									<input name="brokerAuthentication[trustStoreLocation]"
										type="text-input-box"
										ref={register({required: true})}
										className="text-input-box"
										data-lpignore="true"
										placeholder="The TrustStore Location">
									</input>
								</div>
								<div className="flex mt20">
									<label className="input-label">TrustStore Password</label>
										<input name="brokerAuthentication[trustStorePassword]"
											type="password"
											ref={register({required: false})}
											className="text-input-box"
											data-lpignore="true"
											placeholder="The TrustStore password, if any">
										</input>
								</div>
							</div>
						}
					</div>
					<div className="mt20">
						<div className="pl200">
							<CheckBox checked={hasKeyStore} onChange={toggleKeyStore} title={'Add Keystore'}></CheckBox>
						</div>
						{hasKeyStore &&
							<div>
								<div className="flex mt20">
									<label className="input-label">KeyStore Location</label>
									<input name="brokerAuthentication[keyStoreLocation]"
										type="text-input-box"
										ref={register({required: true})}
										className="text-input-box"
										data-lpignore="true"
										placeholder="The Keystore Location">
									</input>
								</div>
								<div className="flex mt20">
									<label className="input-label">KeyStore Password</label>
										<input name="brokerAuthentication[keyStorePassword]"
											type="password"
											ref={register({required: false})}
											className="text-input-box"
											data-lpignore="true"
											placeholder="The Keystore password, if any">
										</input>
								</div>
								<div className="flex mt20">
									<label className="input-label">Key Password</label>
										<input name="brokerAuthentication[keyPassword]"
											type="password"
											ref={register({required: false})}
											className="text-input-box"
											data-lpignore="true"
											placeholder="The Key password, if any">
										</input>
								</div>
							</div>
						}
					</div>
					<div className="flex mt20">
						<label className="input-label">Client Protocol</label>
							<input name="brokerIdentity[clientProtocol]"
							type="text"
							ref={register({required: true})}
							className="text-input-box"
							data-lpignore="true"
							placeholder="Client Protocol">
						</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Client Host</label>
							<input name="brokerIdentity[clientHost]"
								type="text"
								ref={register({required: true})}
								className="text-input-box"
								data-lpignore="true"
								placeholder="Client Host">
						</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Semp Host</label>
							<input name="brokerIdentity[sempHost]"
								type="text"
								ref={register({required: true})}
								className="text-input-box"
								data-lpignore="true"
								   placeholder="Semp Host">
							</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Messaging Port</label>
							<input name="brokerIdentity[messagingPort]"
								type="text"
								ref={register({required: true})}
								className="text-input-box"
								data-lpignore="true"
								placeholder="e.g. 55443">
						</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Message VPN</label>
							<input name="brokerIdentity[messageVpn]"
								type="text"
								ref={register({required: true})}
								className="text-input-box"
								data-lpignore="true"
								placeholder="e.g. myMsgVpn">
							</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Semp Port</label>
							<input name="brokerIdentity[sempPort]"
								type="text"
								ref={register({required: true})}
								className="text-input-box"
								data-lpignore="true"
								placeholder="e.g. 943">
							</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Semp Scheme</label>
						<div tabIndex="0" id="reg-ssl-dropdown" className="dropdown-container" onClick={focus} >
							<Dropdown className="dropdown" options={sempSchemes} onChange={setSempSchemeType} value={sempSchemeType} />
						</div>
					</div>
					<div className="title mt20">Event Scan Details</div>
					<div className="flex mt20">
						<label className="input-label align-start">Topics Subscriptions</label>
						<textarea
							name="discoveryOperation[subscriptionSet]"
							ref={register({ required: true })}
							className="text-area text-height"
							placeholder="my/topic
my/other/topic
#P2P/QUE/>
>"
						></textarea>
					</div>
						<div className="form-footer">
						<SolaceButton
							style={{ marginRight: "8px" }}
							kind="call-to-action"
							title="Start Scan">
								<input type="submit"></input>
						</SolaceButton>
					</div>
				</form>
			</div>
		</div>
		</>
	)
}

export default SolaceRuntimePlugin;

