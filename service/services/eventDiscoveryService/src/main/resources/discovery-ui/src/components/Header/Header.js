import React from 'react';
import './Header.scss';
import logo from '../../assets/svg/discovery.svg';

const Header = (props) => {

	
	return (<div className={`header ${props.headerClass ? props.headerClass: ''}`}>
		<div className="content">
			<div className="title">
				<img src={logo}
					alt="discovery-icon"
					className="discovery-icon">
				</img>
				Runtime Discovery
			</div>
		</div>
	</div>
	
	)
}
	



export default Header;