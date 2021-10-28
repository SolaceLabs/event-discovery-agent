import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import './KafkaPlugin.scss';
import Dropdown from 'react-dropdown';
import CheckBox from '../../../CheckBox/CheckBox';
import 'react-dropdown/style.css';
import logo from '../../../../assets/svg/discovery.svg';
import '../../../Header/Header.scss';
import PLUGINS from '../../../../../src/assets/plugins/plugins';
import SolaceButton from '../../../../components/SolaceButton/SolaceButton';

import backArrow from '../../../../assets/svg/back-arrow.svg';

// this form is shared between Kafka, Confluent and Amazon MSK
const KafkaPlugin = (props) => {

	const authOptions = [
		{ value: 'NOAUTH', label: 'No Auth' },
		{ value: 'SASL_PLAIN', label: 'SASL Plain' },
		{ value: 'SASL_GSSAPI', label: 'SASL GSSAPI (kerberos)' },
		{ value: 'SASL_SCRAM_256', label: 'SASL SCRAM 256' },
		{ value: 'SASL_SCRAM_512', label: 'SASL SCRAM 512' },

	];
	const defaultAuthOption = authOptions[0];

	const SSLOptions = [
		{ value: 'SSL_TRANSPORT', label: 'TLS Transport' },
		{ value: 'SSL_TRANSPORT_AUTH', label: 'TLS Transport and Mutual Authentication' }
	];

	const connectorAuthOptions = [
		{ value: 'NOATUH', label: 'No Auth' },
		{ value: 'BASIC_AUTH', label: 'Basic Authentication' }
	];

	const [scanAll, setScanAll] = useState(false);
	const [auth, setAuth] = useState(defaultAuthOption);
	const [SSLType, setSSLType] = useState(SSLOptions[0]);
	const [connectorAuth, setConnectorAuth] = useState(connectorAuthOptions[0]);
	const [connectorSSLType, setConnectorSSLType] = useState(SSLOptions[0]);
	const [registryAuth, setRegistryAuth] = useState(connectorAuthOptions[0]);
	const [registrySSLType, setRegistrySSLType] = useState(SSLOptions[0]);
	const [hasAuthSSL, setHasAuthSSL] = useState(false);
	const [hasConnectorSSL, setHasConnectorSSL] = useState(false);
	const [hasRegistrySSL, setHasRegistrySSL] = useState(false);
	const [hasConnector, setHasConnector] = useState(false);

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
		if (data.brokerAuthentication) {
			data.brokerAuthentication.authenticationType = auth.value;

			if (data.brokerAuthentication.trustStorePassword) {
				data.brokerAuthentication.transportType = "SSL";
			}
		}
		data.brokerIdentity.host += ":" + data.port; 
		console.log(data.brokerIdentity.host);
		props.onSubmit(data);
	}

	const toggleAuthSSL = () => {
		setHasAuthSSL(!hasAuthSSL);
		setSSLType(SSLOptions[0]);
	}

	const toggleConnectorSSL = () => {
		setHasConnectorSSL(!hasConnectorSSL);
		setConnectorSSLType(SSLOptions[0]);
	}

	const toggleRegistrySSL = () => {
		setHasRegistrySSL(!hasRegistrySSL);
	}

	const toggleConnector = () => {
		setHasConnector(!hasConnector);
	}

	const focus = (e) => {
		document.getElementById(e.currentTarget.id).focus();
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
				<div className="title mt20">Target Cluster Authentication</div>
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
						<label className="input-label">Host</label>
						<input name="brokerIdentity[host]"
							type="text"
							ref={register({required: true})}
							className="text-input-box"
							data-lpignore="true">
						</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Port</label>
						<input name="port"
							type="number"
							ref={register({required: true})}
							className="text-input-box short"
							data-lpignore="true">
						</input>
					</div>
					<div className="flex mt20">
						<label className="input-label">Authentication</label>
						<div tabIndex="0" id="auth-dropdown" className="dropdown-container" onClick={focus} >
							<Dropdown className="dropdown" options={authOptions} onChange={setAuth} value={auth}/>
						</div>
					</div>
					{ !["NOAUTH", "SASL_GSSAPI"].includes(auth.value) &&
						<div>
							<div className="flex">
								<label htmlFor="adminUsername" className="input-label">Admin Username</label>
								<input name="brokerAuthentication[adminUsername]"
									type="text"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true">
								</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">Admin Password</label>
								<input name="brokerAuthentication[adminPassword]"
									type="password"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true"
									autoComplete="off">
								</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">Consumer Username</label>
								<input name="brokerAuthentication[consumerUsername]"
									type="text"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true">
								</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">Consumer Password</label>
								<input name="brokerAuthentication[consumerPassword]"
									type="password"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true"
									autoComplete="off">
								</input>
							</div>
						</div>
					}
					{auth.value === "SASL_GSSAPI" &&
						<div>
							<div className="flex mt20">
								<label className="input-label">Principal</label>
								<input name="brokerAuthentication[principal]"
									type="text"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true">
								</input>
							</div>
                                                        <div className="flex mt20">
                                                                <label className="input-label">Service Name</label>
                                                                <input name="brokerAuthentication[serviceName]"
                                                                        type="text"
                                                                        ref={register({ required: true })}
                                                                        className="text-input-box"
                                                                        data-lpignore="true">
                                                                </input>
                                                        </div>
							<div className="flex mt20">
								<label className="input-label">Key Tab Location</label>
								<input name="brokerAuthentication[keyTabLocation]"
									type="password"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true"
									autoComplete="off">
								</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">KRB5 Config Location</label>
								<input name="brokerAuthentication[krb5ConfigurationLocation]"
									type="text"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true">
								</input>
							</div>
						</div>
					}
					<div className="mt20">
						<div className="pl200">
							<CheckBox checked={hasAuthSSL} onChange={toggleAuthSSL} title={'Add TLS Authentication'}></CheckBox>
						</div>
						{hasAuthSSL &&
							<div>
								<div className="flex mt20">
								<label className="input-label">Authentication</label>
								<div tabIndex="0" id="ssl-dropdown" className="dropdown-container" onClick={focus} >
									<Dropdown className="dropdown" options={SSLOptions} onChange={setSSLType} value={SSLType}/>
								</div>
								</div>
								<div className="flex mt20">
									<label htmlFor="adminUsername" className="input-label">Trust Store Location</label>
									<input name="brokerAuthentication[trustStoreLocation]"
										type="text"
										ref={register({ required: true })}
										className="text-input-box"
										data-lpignore="true">
									</input>
								</div>
								<div className="flex mt20">
										<label className="input-label">Trust Store Password</label>
										<input name="brokerAuthentication[trustStorePassword]"
											type="password"
											ref={register({ required: true })}
											className="text-input-box"
											data-lpignore="true"
											autoComplete="off">
									</input>
							</div>
							{SSLType.value === "SSL_TRANSPORT_AUTH" &&
								<div>
										<div className="flex mt20">
											<label htmlFor="adminUsername" className="input-label">Key Store Location</label>
											<input name="brokerAuthentication[keyStoreLocation]"
												type="text"
												ref={register({ required: true })}
												className="text-input-box"
												data-lpignore="true">
											</input>
										</div>
										<div className="flex mt20">
											<label className="input-label">Key Store Password</label>
											<input name="brokerAuthentication[keyStorePassword]"
												type="password"
												ref={register({ required: true })}
												className="text-input-box"
												data-lpignore="true"
												autoComplete="off">
											</input>
										</div>
										<div className="flex mt20">
											<label className="input-label">Key Password</label>
											<input name="brokerAuthentication[keyPassword]"
												type="password"
												ref={register({ required: true })}
												className="text-input-box"
												data-lpignore="true"
												autoComplete="off">
											</input>
										</div>
								</div>
							}
					</div>
				}
				</div>
				<div className="flex-column mt20">
					<div className="flex-space">
						<div className="sub-title">Connectors Authentication <span className="optional">(Optional)</span></div>
						{!hasConnector && <a className="flex-center" onClick={toggleConnector}>Add Connector</a>}
					</div>
						
				{hasConnector && 
					<div>
						<div className="flex mt20">
							<label className="input-label">Host</label>
							<div>
								<input name="brokerIdentity[connector[hostname]]"
									type="text"
									ref={register({required: true})}
									className="text-input-box"
									data-lpignore="true">
								</input>
								<a className="f-20 ml5" onClick={() => setHasConnector(false)}>
									<i className="fas fa-trash-alt" aria-hidden="true"></i>
								</a>
							</div>	
						</div>
						<div className="flex mt20">
							<label className="input-label">Port</label>
							<input name="brokerIdentity[connector[port]]"
								type="number"
								ref={register({required: true})}
								className="text-input-box short"
								data-lpignore="true">
							</input>
						</div>
						<div className="flex mt20">
							<label className="input-label">Authentication</label>
							<div tabIndex="0" id="con-auth-dropdown" className="dropdown-container" onClick={focus} >
							<Dropdown className="dropdown" options={connectorAuthOptions} onChange={setConnectorAuth} value={connectorAuth}/>
							</div>
						</div>
					{connectorAuth.value === "BASIC_AUTH" && 
						<div>
							<div className="flex mt20">
								<label className="input-label">Username</label>
								<input name="brokerAuthentication[connector[basicAuthUsername]]"
									type="text"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true">
								</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">Password</label>
								<input name="brokerAuthentication[connector[basicAuthPassword]]"
									type="password"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true"
									autoComplete="off">
								</input>
							</div>
						</div>
					}
						<div className="pl200 mt40">
								<CheckBox checked={hasConnectorSSL} onChange={toggleConnectorSSL} title={'Add TLS Authentication'}></CheckBox>
						</div>
						{hasConnectorSSL &&
						<div>
							<div className="flex mt20">
								<label className="input-label">Authentication</label>
								<div tabIndex="0" id="con-ssl-dropdown" className="dropdown-container" onClick={focus} >
									<Dropdown className="dropdown" options={SSLOptions} onChange={setConnectorSSLType} value={connectorSSLType}/>
								</div>
							</div>
							<div className="flex mt20">
								<label className="input-label">Trust Store Location</label>
								<input name="brokerAuthentication[connector[trustStoreLocation]]"
									type="text"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true">
								</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">Trust Store Password</label>
								<input name="brokerAuthentication[connector[trustStorePassword]]"
									type="password"
									ref={register({ required: true })}
									className="text-input-box"
									data-lpignore="true"
									autoComplete="off">
								</input>
							</div>
						{connectorSSLType.value === "SSL_TRANSPORT_AUTH" &&
							<div>
								<div className="flex mt20">
									<label htmlFor="adminUsername" className="input-label">Key Store Location</label>
									<input name="brokerAuthentication[connector[keyStoreLocation]]"
										type="text"
										ref={register({ required: true })}
										className="text-input-box"
										data-lpignore="true">
									</input>
								</div>
								<div className="flex mt20">
									<label className="input-label">Key Store Password</label>
									<input name="brokerAuthentication[connector[keyStorePassword]]"
										type="password"
										ref={register({ required: true })}
										className="text-input-box"
										data-lpignore="true"
										autoComplete="off">
									</input>
								</div>
								<div className="flex mt20">
									<label className="input-label">Key Password</label>
									<input name="brokerAuthentication[connector[keyPassword]]"
										type="password"
										ref={register({ required: true })}
										className="text-input-box"
										data-lpignore="true"
										autoComplete="off">
									</input>
								</div>
							</div>
						}
					</div>
				}	
					</div>
				}
					</div>
					{props.plugin === 'confluent' && <div className="flex-column mt20">
						<div className="flex-space">
							<div className="sub-title">Schema Registry Authentication</div>
						</div>
						<div>
							<div className="flex mt20">
								<label className="input-label">Host</label>
								<div>
									<input name="brokerIdentity[schemaRegistry[hostname]]"
										type="text"
										ref={register({ required: true })}
										className="text-input-box"
										data-lpignore="true">
									</input>
								</div>
							</div>
							<div className="flex mt20">
								<label className="input-label">Port</label>
								<input name="brokerIdentity[schemaRegistry[port]]"
									type="number"
									ref={register({ required: true })}
									className="text-input-box short"
									data-lpignore="true">
								</input>
							</div>
							<div className="flex mt20">
								<label className="input-label">Authentication</label>
								<div tabIndex="0" id="con-auth-dropdown" className="dropdown-container" onClick={focus} >
									<Dropdown className="dropdown" options={connectorAuthOptions} onChange={setRegistryAuth} value={registryAuth} />
								</div>
							</div>
							{registryAuth.value === "BASIC_AUTH" &&
								<div>
									<div className="flex mt20">
										<label className="input-label">Username</label>
										<input name="brokerAuthentication[schemaRegistry[basicAuthUsername]]"
											type="text"
											ref={register({ required: true })}
											className="text-input-box"
											data-lpignore="true">
										</input>
									</div>
									<div className="flex mt20">
										<label className="input-label">Password</label>
										<input name="brokerAuthentication[schemaRegistry[basicAuthPassword]]"
											type="password"
											ref={register({ required: true })}
											className="text-input-box"
											data-lpignore="true"
											autoComplete="off">
										</input>
									</div>
								</div>
							}
							<div className="pl200 mt40">
								<CheckBox checked={hasRegistrySSL} onChange={toggleRegistrySSL} title={'Add TLS Authentication'}></CheckBox>
							</div>
							{hasRegistrySSL &&
								<div className="mb20">
									<div className="flex mt20">
										<label className="input-label">Authentication</label>
										<div tabIndex="0" id="reg-ssl-dropdown" className="dropdown-container" onClick={focus} >
											<Dropdown className="dropdown" options={SSLOptions} onChange={setRegistrySSLType} value={registrySSLType} />
										</div>
									</div>
									<div className="flex mt20">
										<label className="input-label">Trust Store Location</label>
										<input name="brokerAuthentication[schemaRegistry[trustStoreLocation]]"
											type="text"
											ref={register({ required: true })}
											className="text-input-box"
											data-lpignore="true">
										</input>
									</div>
									<div className="flex mt20">
										<label className="input-label">Trust Store Password</label>
										<input name="brokerAuthentication[schemaRegistry[trustStorePassword]]"
											type="password"
											ref={register({ required: true })}
											className="text-input-box"
											data-lpignore="true"
											autoComplete="off">
										</input>
									</div>
									{registrySSLType.value === "SSL_TRANSPORT_AUTH" &&
										<div>
											<div className="flex mt20">
												<label className="input-label">Key Store Location</label>
												<input name="brokerAuthentication[schemaRegistry[keyStoreLocation]]"
													type="text"
													ref={register({ required: true })}
													className="text-input-box"
													data-lpignore="true">
												</input>
											</div>
											<div className="flex mt20">
												<label className="input-label">Key Store Password</label>
												<input name="brokerAuthentication[schemaRegistry[keyStorePassword]]"
													type="password"
													ref={register({ required: true })}
													className="text-input-box"
													data-lpignore="true"
													autoComplete="off">
												</input>
											</div>
											<div className="flex mt20">
												<label className="input-label">Key Password</label>
												<input name="brokerAuthentication[schemaRegistry[keyPassword]]"
													type="password"
													ref={register({ required: true })}
													className="text-input-box"
													data-lpignore="true"
													autoComplete="off">
												</input>
											</div>
										</div>
									}
								</div>
							}
						</div>
					</div>}
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

export default KafkaPlugin;

