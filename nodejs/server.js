var FCM = require('fcm-push'),
	Convert = require('xml-js'),
	Express = require('express'),
	BodyParser = require('body-parser'),
	Settings = require('./settings.json'),
	server = Express(),
	fcm = new FCM(Settings.serverToken),
	fcmDestination = '/topics/' + Settings.phoneToken;


function parseSoap(xml) {

	var result = [],
		trimBoth = value => String(value).replace(/^\s+/, '').replace(/\s+$/, '');
		isValidPhoneNumber = number => /^\+[0-9]+$/.test(number);

	if (typeof xml === 'string') try {
		
		xml = Convert.xml2js(xml, {
			compact: true,
			trim: true,
			alwaysChildren: true,
			ignoreDeclaration: true,
			ignoreAttributes: true,
			ignoreComment: true,
			ignoreCdata: true
		}).SMS;

		do {
			if (typeof xml !== 'object') break;
			var text = xml.message.text._text, numbers = xml.numbers.number;
			if (typeof text !== 'string' || typeof numbers !== 'object') break;
			text = trimBoth(text);
			if (!text.length) break;
			numbers = [].concat(numbers).map(number => number._text);
			if (!numbers.length) break;
			if (numbers.some(number => typeof number !== 'string')) break;
			numbers = numbers.map(number => trimBoth(number));
			if (numbers.some(number => !isValidPhoneNumber(number))) break;
			result = [text, numbers];
		} while (0);
		
	} catch (e) {}
	return result;
}

function handleResponse(response, status) {
	response.end(`<?xml version="1.0" encoding="UTF-8"?>
		<RESPONSE>
			<status>${status ? '1' : '-4'}</status>
			<credits>0</credits>
		</RESPONSE>
	`);
}


server.use(BodyParser.urlencoded({extended: false}));

server.post('/', function(request, response) {

	var soapRequest = request.body;
	if (typeof soapRequest === 'object') {
		soapRequest = parseSoap(soapRequest.XML);
	} else soapRequest = [];

	var [text, numbers] = soapRequest;

	if (!text || !numbers) {
		handleResponse(response, false);
	}

	else fcm.send({to: fcmDestination, data: {
		json: JSON.stringify({
			text: text,
			numbers: numbers
		})
	}}, function(error, fcmResponse) {
		if (error) handleResponse(response, false);
		else try {
			fcmResponse = JSON.parse(fcmResponse);
			handleResponse(response, typeof fcmResponse === 'object' && fcmResponse.message_id);
		} catch (exception) { handleResponse(response, false); }
	});

});


server.listen(Settings.portNumber);