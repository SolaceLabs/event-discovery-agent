import React, { useState, useEffect } from 'react';
import './ProgressModalContent.scss';


const ModalContent = (props) => {
	const [isConnected, setIsConnected] = useState(false);
	const [pluginType, setPluginType] = useState("");
	const [displayMsg, setDisplayMsg] = useState("");

	useEffect(() => {
		setIsConnected(props.isConnected);
		if (props.pluginType) {
			setPluginType(props.pluginType);
		}
		if (props.durationInSecs <= 1) {
			setDisplayMsg("The scan will end in 1 second...");
		} else {
			setDisplayMsg("The scan will end in " + props.durationInSecs + " seconds...")
		}
	}, [props.isConnected, props.durationInSecs])

	const getButtons = () => {
		return props.buttons.map((button, index) => {
			return (
				<SolaceButton
					style={{ marginRight: "8px" }}
					key={index}
					disabled={false}
					kind={button.kind}
					title={button.title}
					onClick={button.onClick} 
				/>
			);
		})
	}

	const getStatusText = () => {
		if (isConnected) {
			return "Scanning topic subscriptions..."
		} else {
			return "Connecting to the broker..."
		}
	}

		return (
			<div className="modal-content">
				<div className="flex">
					<div className="status-text">
						{props.icon &&
							<i className={props.icon}></i>
						}
						<span key={props.durationInSecs} className={"ml10"}>
							{(pluginType === 'solace' && props.durationInSecs !== -1) 
							? displayMsg
							: getStatusText()}
						</span>
					</div>
					{isConnected &&
						<div className="button-container">
							{getButtons()}
						</div>
					}
				</div>

				<div>
					Event Discovery agent is scanning for events. 
					Once completed the results will be available for downloading.
				</div>
			</div>

		)

}

export default ModalContent;