import React from 'react';
import './ResultsContent.scss';


const ResultsContent = (props) => {
	const getBodyText = () => {
		if (props.isStopped) {
			return "Event Discovery agent was unable to finish scanning for events."
		} else {
			return `Event Discovery agent has finished scanning 
			for events${props.topics.length > 0 ? '' : ' on all topic subscriptions'}.`
		}
	}

	const getTopics = () => {
		return props.topics.map((topic) => {
			return (
				<div key={topic}>
					{topic}
				</div> 
			);
		})
	}

		return (
			<div className="results-content">
				<div>{getBodyText()}</div>
				<div>
					{props.topics.length > 0 &&
						<div>
							{`Topic Subscriptions(${props.topics.length})`}
							<div className="topics">
								{getTopics()}
							</div>
						</div>
					}
				</div>
			</div>

		)

}

export default ResultsContent;