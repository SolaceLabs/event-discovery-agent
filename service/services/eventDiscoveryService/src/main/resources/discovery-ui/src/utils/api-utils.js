import axios from 'axios';
import { environment } from '../env';

const headers = {
	"Content-Type": "application/json; charset=UTF-8"
}

export const ENDPOINTS = {
	DISCOVERY: "event-discovery-agent/local/app/operation",
	USER: "event-discovery-agent/local/user"
}

export const getDiscoveryResults = async (jobId) => {
	axios.defaults.timeout = 30000;
	const path = `${environment.host}/api/v0/${ENDPOINTS.DISCOVERY}/${jobId}/result/asyncapi`;
	try {
		return await axios.get(path, { headers });
	}
	catch (err) {
		return console.log(err);
	}
}

export const startDiscoverScan = async (data) => {
	const path = `${environment.host}/api/v0/${ENDPOINTS.DISCOVERY}`;
	const body = data;
	try {
		const { data } = await axios.post(path, body, { headers });
		return data.data;
	}
	catch (err) {
		return console.log(err);
	}
}

export const getDiscoverStatus = async (jobId) => {
	const path = `${environment.host}/api/v0/${ENDPOINTS.DISCOVERY}/${jobId}/status`;
	try {
		return await axios.get(path, { headers });
	}
	catch (err) {
		return console.log(err);
	}
}

export const stopDiscoveryScan = async (jobId) => {
	const path = `${environment.host}/api/v0/${ENDPOINTS.DISCOVERY}/${jobId}/stop`;
	try {
		return await axios.post(path, { headers });
	}
	catch (err) {
		return console.log(err);
	}

}

export const cancelDiscoveryScan = async (jobId) => {
	const path = `${environment.host}/api/v0/${ENDPOINTS.DISCOVERY}/${jobId}/cancel`;
	try {
		return await axios.post(path, { headers });
	}
	catch (err) {
		return console.log(err);
	}
}