import React, { useState, useEffect } from 'react';
import Modal from 'react-modal';
import SolaceButton from '../../components/SolaceButton/SolaceButton';
import './SolaceModal.scss';


const SolaceModal = (props) => {
	const [isOpen, setIsOpen] = useState(false);
	const [modalContent, setModalContent] = useState({});


	useEffect(() => {
		setIsOpen(props.isOpen);
		setModalContent(props.content);
	}, [props.isOpen, props.content])


	const getButtons = () => {
		return props.buttons.map((button, index) => {
			return (
				<SolaceButton
					style={{ marginRight: "8px" }}
					key={index}
					disabled={button.disabled}
					kind={button.kind}
					title={button.title}
					onClick={button.onClick} 
				/>
			);
		})
	}

	return (
		<Modal isOpen={isOpen}
			ariaHideApp={false}
			className="scan-modal modal"
			overlayClassName="scan-modal overlay">
			<div className="scan-modal">
				<div className="modal-header">
					{props.headerIcon &&
						<i className={props.headerIcon}></i>
					}
					<div className={props.headerIcon && "ml10"}>{props.header}</div>
				</div>
				<div className="modal-body">
					{modalContent}
				</div>
				<div className="footer">
					{getButtons()}
				</div>

			</div>
		</Modal>
	)


}

export default SolaceModal;