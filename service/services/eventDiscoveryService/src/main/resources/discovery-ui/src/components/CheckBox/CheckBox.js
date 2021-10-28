import React from 'react';
import './CheckBox.scss';


const CheckBox = (props) => {
	return (
		<div className="solace-checkbox" onClick={props.onChange}>
			<div className="container">
				<input type="checkbox" onChange={() => {}} checked={props.checked}></input>
				<span className="title">{props.title}</span>
				<span className="checkmark"></span>
			</div>
		</div>
	)
	

}

export default CheckBox;