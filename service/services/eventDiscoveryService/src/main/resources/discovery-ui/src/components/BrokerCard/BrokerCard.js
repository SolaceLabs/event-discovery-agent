import React from 'react';
import { Link } from "react-router-dom";
import './BrokerCard.scss';


const BrokerCard = (props) => (
	<div>
		<Link to={props.route}>
			<div className={`broker-card ${props.logoClass ? props.logoClass : ''}`}>
				<div className="details">
					<div className="broker-title">
						{props.title}
					</div>
					<div className="broker-description">{props.description}</div>
				</div>
				<div className="broker-logo-container">
					<img src={props.logo}
						alt="broker-icon"
						className={`broker-logo ${props.logoClass? props.logoClass : ''}`}>
					</img>
				</div>
			</div>
		</Link>
	</div>

)


export default BrokerCard;