import React, { useState, useEffect, useCallback, useRef } from 'react';
import SolaceModal from '../SolaceModal/SolaceModal';
import './PluginContainer.scss';
import ResultsContent from '../ResultsContent/ResultsContent';
import ProgressModalContent from '../ProgressModalContent/ProgressModalContent';
import '../Header/Header.scss'

import {
	startDiscoverScan,
	getDiscoveryResults,
	getDiscoverStatus
} from '../../utils/api-utils';
import {
	createDTOWithDefaultValues,
	downloadJSONFile,
	createSubscriptionSet
} from '../../utils/common-utils';
import KafkaPlugin from './Plugins/KafkaPlugin/KafkaPlugin';
import SolaceRuntimePlugin from './Plugins/SolaceRuntimePlugin/SolaceRuntimePlugin';
import HiveMQPlugin from './Plugins/HiveMQPlugin/HiveMQPlugin';
import RABBITMQPlugin from './Plugins/RABBITMQPlugin/RABBITMQPlugin';
import NATSPlugin from './Plugins/NATSPlugin/NATSPlugin';
import { PLUGIN_DEFAULTS } from '../../assets/plugins/plugins';


const PluginContainer = (props) => {
	const [isConnected, setIsConnected] = useState(false);

	const getProgressModalContent = (isConnected, durationInSecs = -1) => {
		return <ProgressModalContent isConnected={isConnected}
			pluginType={props.plugin}
			durationInSecs={durationInSecs}
			buttons={[]}
			icon={"icon success fa-3x fas fa-spinner fa-fw fa-pulse"}/>
	}

	const getProgressModalButtons = () => {
		return [
			{
				title: "Download Results",
				disabled: true,
				kind: "outline",
				onClick: () => getResults()
			}
		]
	}

	const getModalHeader = (s) => {
		if (props.plugin === "solace") {
			return "Topic Subscription Scan " + s;
		}
		return "Runtime Discovery Scan " + s;
	}

	const progressModal = {
		headerIcon: "",
		header: getModalHeader("in Progress"),
		content: getProgressModalContent(false),
		buttons: getProgressModalButtons()
	}

	const getResultModalContent = (topics, isStopped) => {
		return <ResultsContent isStopped={isStopped}
			topics={topics}
			link={resultModal.link}
		/>
	}

	const resultModal = {
		headerIcon: "icon success fas fa-check-circle",
		header: getModalHeader("is Complete"),
		content: "",
		topics: topics,
		link: {
			title: "PubSub+ Could Event Portal Discovery Feature",
			href: "https://console.solace.cloud/event-discovery",
			class: 'fas fa-external-link-alt'
		},
		buttons: [
			{
				title: "Close",
				disabled: false,
				kind: "text",
				onClick: () => {
					setIsScanModalOpen(false)
				}
			},
			{
				title: "Download Results",
				disabled: false,
				kind: "outline",
				onClick: () => getResults()
			},

		]
	}


	const [jobId, setJobId] = useState("");
	const [isErrorModalOpen, setIsErrorModalOpen] = useState(false);
	const [isScanModalOpen, setIsScanModalOpen] = useState(false);
	const [isScanStopped, setIsScanStopped] = useState(false);
	const [error, setError] = useState("");
	const [isScanCompleted, setIsScanCompleted] = useState(false);
	const [topics, setTopics] = useState([]);
	const [modalData, setModalData] = useState(progressModal);
	const [modalContent, setModalContent] = useState(progressModal.content);
	const [setDuration] = useState(0);
	const [isScanCanceled, setIsScanCanceled] = useState(false);
	const polling = useRef();
	const scanTimeout = useRef();

	useEffect(() => {
		if (jobId && isConnected) {
			if (!isScanStopped && !isScanCanceled) {
				polling.current = setInterval(() => {
					pollJobStatus(jobId)
						.then(({ data }) => {
							if (data.status === "COMPLETED" || data.status === "STOPPED") {
								setIsScanCompleted(true);
								clearInterval(polling.current);
							} if (data.status === "ERROR") {
								let errorMessage = "Unable to complete the scan.";
								if (data.error !== undefined) {
									errorMessage = data.error;
								}
								setError(errorMessage);
								setIsScanModalOpen(false);
								setIsErrorModalOpen(true);
								clearInterval(polling.current);
							}
					}
					)
				}, 2000);
				
			}
			
			if (isScanStopped) {
				stopScan();
				clearTimeout(scanTimeout.current);
				if (props.plugin === "solace") {
					return setIsScanModalOpen(false);
				}
				resultModal.headerIcon = "icon warning fas fa-exclamation-triangle";
				resultModal.header =  getModalHeader("was Stopped");
				resultModal.content = getResultModalContent(topics, isScanStopped);	
				setTimeout(() => {
					switchToResultsModal();
				}, 500);
			}

			if (isScanCanceled) {
				cancelScan();
				clearInterval(polling.current);
				clearTimeout(scanTimeout.current);
				return setIsScanModalOpen(false);
			}

			if (isScanCompleted && !isScanStopped) {
				if (props.plugin === "solace" && !isScanCanceled) {
					clearInterval(polling.current);
					clearTimeout(scanTimeout.current);
					showResults();
					return setIsScanModalOpen(false);
				}
				resultModal.content = getResultModalContent(topics, isScanStopped);
				switchToResultsModal();
			}
		} 
	}, [jobId, isConnected, isScanCompleted, switchToResultsModal, topics, isScanStopped, isScanCanceled])

	useEffect(() => {
		if (jobId) {
			if (!isConnected) {
				setIsConnected(true);
				progressModal.content = getProgressModalContent(true);
				setModalData(progressModal);
				setModalContent(progressModal.content);
			}
		}
	}, [jobId, isConnected, progressModal])

	const defaultValues = PLUGIN_DEFAULTS[props.plugin];
	
	const initializeStates = () => {
		setJobId("");
		setIsScanCompleted(false);
		setIsScanCanceled(false);
		setIsConnected(false);
		setIsScanStopped(false);
	}

	const switchToResultsModal = useCallback(() => {
			setModalData(resultModal);
			setModalContent(resultModal.content)
	}, [resultModal]);
	


	const onSubmit = async (data) => {
		initializeStates();
		setIsScanModalOpen(true);
		setModalData(progressModal);
		setModalContent(progressModal.content);
		if (data.discoveryOperation) {
			data.discoveryOperation.subscriptionSet = createSubscriptionSet(data.discoveryOperation.subscriptionSet, defaultValues)
			setTopics(data.discoveryOperation.subscriptionSet);
			if (props.plugin === "solace") {
				return promptForScan(data);
			}
		}

		if (data.brokerIdentity.schemaRegistry) {
			data.brokerIdentity.schemaRegistry.port = parseFloat(data.brokerIdentity.schemaRegistry.port);
		}

		if (data.brokerIdentity.connector) {
			data.brokerIdentity.connector.port = parseFloat(data.brokerIdentity.connector.port);
		}
		const jobData = await startDiscoverScan(createDTOWithDefaultValues(defaultValues, data));
		if (jobData && jobData.jobId) {
			setJobId(jobData.jobId);
		} else {
			setError("Unable to connect to broker.");
			setIsScanModalOpen(false);
			setIsErrorModalOpen(true);
		}
	}

	const getListSubscriptions = (list) => {
		if (list.length > 0) {
			let index = 0
			const subscribtionList = list.map((subscription) => 
				<li key={index += 1}>{subscription}</li>
			);
			return (
				<div>
					<ul className="subscription-list">{subscribtionList}</ul>
				</div>
			);
		}
	}

	const getPromptModalContent = (data) => {
		return (
			<div>
				<p>You are about to start a run-time Event Discovery scan against the following topic subscriptions:
				</p>
				{getListSubscriptions(data.discoveryOperation.subscriptionSet)}
				<p>
					Performing an Event Discovery scan will attract all events published by applications on the associated topics to the targeted event broker.
					As such, the use of <a href="https://docs.solace.com/PubSub-Basics/Wildcard-Charaters-Topic-Subs.htm" target="_blank">topic subscription wild-carding </a>
					should be used with caution, especially when targeting Production broker services or services that are connected with <a href="https://docs.solace.com/Overviews/DMR-Overview.htm" target="_blank">
					Dynamic Message Routing </a>
					links within an event mesh.
				</p>
			</div>
		)
	}

	const promptModal = (data) => {
		return {
			headerIcon: "",
			header: "WARNING",
			buttons: [
				{
					title: "Cancel",
					kind: "text",
					onClick: () => setIsScanModalOpen(false)
				},
				{
					title: "Continue",
					kind: "outline",
					onClick: () => startTopicScan(data)
				}
			],
		}
	}

	const promptForScan = (data) => {
		setModalData(promptModal(data));
		setModalContent(getPromptModalContent(data));
	}

	const startTopicScan = async (data) => {
		let clientProtocol = "tcp";
		let clientPort = "55555";
		let hostname = "";
		if (data.brokerIdentity && data.brokerAuthentication && data.discoveryOperation) {
			let url = data.brokerIdentity.url
			if (url.includes("://")) {
				clientProtocol = url.split("://")[0];
				var urlWithoutProtocol = url.split("://")[1];
				if (urlWithoutProtocol.includes(":")) {

					// URL has protocol and port
					hostname = urlWithoutProtocol.split(":")[0];
					clientPort = urlWithoutProtocol.split(":")[1];
				} else {

					// URL has protocol but no port
					hostname = urlWithoutProtocol;
				}
			} else {
				if (url.includes(":")) {
					// URL has no protocol but has port
					hostname = url.split(":")[0];
					clientPort = url.split(":")[1];
				} else {
					// URL has no protocol or port
					hostname = url;
				}
			}


			let discoveryData = {
				brokerIdentity: {
					brokerType: "SOLACE",
					clientProtocol: clientProtocol,
					clientHost: hostname,
					messagingPort: clientPort,
					messageVpn: data.brokerIdentity.messageVpn
				},
				brokerAuthentication: {
					brokerType: "SOLACE",
					clientUsername: data.brokerAuthentication.clientUsername,
					clientPassword: data.brokerAuthentication.clientPassword
				},
				discoveryOperation: {
					operationType: "topicAnalysis",
					durationInSecs: data.discoveryOperation.durationInSecs,
					subscriptionSet: data.discoveryOperation.subscriptionSet
				}
			}

			const jobData = await startDiscoverScan(createDTOWithDefaultValues(defaultValues, discoveryData))
			processStartTopicScanResponse(jobData);
		}
	}

	const processStartTopicScanResponse = (data) => {
		if (data) {
			setDuration(data.discoveryOperation ? data.discoveryOperation.durationInSecs : -1);
			if (typeof data.jobId === 'undefined') {
				setJobId("undefined");
			} else {
				setJobId(data.jobId);
			}
		} else {
			setError("Unable to connect to broker.");
			setIsScanModalOpen(false);
			setIsErrorModalOpen(true);
		}
	}

	const getResults = async () => {
		const { data } = await getDiscoveryResults(jobId);
		downloadJSONFile(data, 'title');
	}


	const pollJobStatus = useCallback(async (jobId) => {
		const { data } = await getDiscoverStatus(jobId);
		return data;
	}, []);


	return (
		<div>
			{props.plugin === 'kafka' && <KafkaPlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
			{props.plugin === 'confluent' && <KafkaPlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
			{props.plugin === 'amazon' && <KafkaPlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
			{props.plugin === 'solaceRuntime' && <SolaceRuntimePlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
			{props.plugin === 'hiveMQRuntime' && <HiveMQPlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
			{props.plugin === 'RABBITMQRuntime' && <RABBITMQPlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
			{props.plugin === 'natsRuntime' && <NATSPlugin plugin ={props.plugin} onSubmit={onSubmit}/>}
			<SolaceModal
						isOpen={isScanModalOpen}
						headerIcon={modalData.headerIcon}
						link={modalData.link}
						header={modalData.header}
						content={modalContent}
						buttons={modalData.buttons}>
			</SolaceModal>
			<SolaceModal
						isOpen={isErrorModalOpen}
						headerIcon={"fas fa-exclamation-circle icon danger"}
						header={"Error"}
						content={error}
						buttons={[{
							title: "Close",
							disabled: false,
							kind: "text",
							onClick: () => setIsErrorModalOpen(false)}]}>
			</SolaceModal>
		</div>
	)
}



export default PluginContainer;
