import "./SolaceButton.scss";
import * as React from "react";


function SolaceButton(props) {
	const { title, type, kind, disabled, id, children, onClick } = props;
	function handleClick(e) {
		if (onClick) {
			onClick(e);
		}
	}

	return (
		<button {...props} id={id} disabled={disabled} className={`react-button ${kind} old-style ${disabled ? "disabled" : ""}`} onClick={handleClick}>
			{title}
		</button>
	);
}

export default SolaceButton;
