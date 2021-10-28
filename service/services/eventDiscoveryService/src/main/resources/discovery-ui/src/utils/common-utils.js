import { saveAs } from 'file-saver';

export const fieldHasError = (errors, key, child) => {
	if (errors[key]) {
		return errors[key][child];
	}
	return false;
}

export const createDTOWithDefaultValues = (defaultValues, formData) => {
	const dto = {}
	const keys = Object.keys(defaultValues);
	keys.forEach((key) => {
		dto[key] = { ...defaultValues[key], ...formData[key] };
	})
	return dto;
}

export const createSubscriptionSet = (string, defaultValues) => {
	return string ? string.split("\n") : defaultValues.discoveryOperation.subscriptionSet;
} 

export const downloadJSONFile = (object, key) => {
	const date = new Date().toISOString().split('T')[0];
	const time = new Date(Date.now()).toLocaleString('en-GB').split(',')[1].replaceAll(':', '.');
	const fileName = `${object.info[key]}-${date}${time}.json`;
	const fileToSave = new Blob([JSON.stringify(object)], {
	  type: 'application/json',
	  name: fileName
	});
	saveAs(fileToSave, fileName);
  }
