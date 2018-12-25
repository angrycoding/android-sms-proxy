var IP = require('ip'),
	FCM = require('fcm-push'),
	Convert = require('xml-js'),
	Express = require('express'),
	BodyParser = require('body-parser'),
	Settings = require('./settings.json'),
	server = Express(),
	fcm = new FCM(Settings.serverToken),
	fcmDestination = '/topics/' + Settings.phoneToken,
	updateStatusURL = `http://${IP.address()}:${Settings.portNumber}/updateStatus`,
	trimBoth = value => String(value).replace(/^\s+/, '').replace(/\s+$/, '');

var DELIVERY_STATUSES = {};

function parseSendOperation(request, ret) {
	var isValidPhoneNumber = number => /^\+[0-9]+$/.test(number);
	
	var text = request.message.text._text, numbers = request.numbers.number;
	if (typeof text !== 'string' || typeof numbers !== 'object') return ret();
	text = trimBoth(text);
	if (!text.length) return ret();
	numbers = [].concat(numbers);
	if (!numbers.length) return ret();

	numbers = numbers.map(function(number) {
		var value = number._text;
		if (typeof value === 'string') value = trimBoth(value);
		var result = {number: value};
		if (number.hasOwnProperty('_attributes')) {
			var attrs = number._attributes;
			if (attrs.hasOwnProperty('messageID')) {
				value = attrs.messageID;
				if (typeof value === 'string') {
					value = trimBoth(value);
					if (value.length) {
						result.id = value;
						result.success = `${updateStatusURL}?id=${value}&status=DELIVERED`;
						result.fail = `${updateStatusURL}?id=${value}&status=NOT_DELIVERED`;
					}
				}
			}
		}
		return result;
	});

	if (numbers.some(function(number) {
		if (typeof number.number !== 'string') return true;
		if (!isValidPhoneNumber(number.number)) return true;
		if (number.hasOwnProperty('success') && typeof number.success !== 'string') return true;
		if (number.hasOwnProperty('fail') && typeof number.fail !== 'string') return true;
	})) return ret();

	ret('SEND', [text, numbers]);
}

function parseGetStatus(request, ret) {
	var numbers = request.statistics.number;
	if (typeof numbers !== 'object') return ret();
	numbers = [].concat(numbers);
	if (!numbers.length) return ret();
	numbers = numbers.map(function(number) {
		var value = number._text;
		if (typeof value === 'string') value = trimBoth(value);
		return value;
	});
	if (numbers.some(number => typeof number !== 'string')) return ret();
	ret('GETSTATUS', numbers);
}

function parseSoap(request, ret) {
	if (!(request instanceof Buffer)) return ret();
	request = request.toString();
	if (typeof request !== 'string') return ret();
	try {
		
		request = Convert.xml2js(request, {
			compact: true,
			trim: true,
			alwaysChildren: true,
			ignoreDeclaration: true,
			ignoreComment: true,
			ignoreCdata: true
		}).SMS;

		if (typeof request !== 'object') return ret();
		var operation = (request.operations || {});
		operation = (operation.operation || {});
		operation = trimBoth(operation._text || '');
		if (operation === 'SEND') return parseSendOperation(request, ret);
		if (operation === 'GETSTATUS') return parseGetStatus(request, ret);
	} catch (e) {}
	ret();
}

function handleResponse(response, status) {
	response.end(`<?xml version="1.0" encoding="UTF-8"?>
		<RESPONSE>
			<status>${status ? '1' : '-4'}</status>
			<credits>0</credits>
		</RESPONSE>
	`);
}


server.use(BodyParser.raw());

server.get('/updateStatus', function(request, response) {
	var query = request.query;
	if (typeof query === 'object') {
		var id = query.id, status = query.status;
		if (typeof id === 'string' && typeof status === 'string') {
			if (DELIVERY_STATUSES.hasOwnProperty(id)) {
				DELIVERY_STATUSES[id] = status;
			}
		}
	}
	response.end('');
});

server.post('*', function(request, response) {
	parseSoap(request.body, function(operation, data) {

		if (operation === 'SEND') {
			var [text, numbers] = data;
			if (!text || !numbers) return handleResponse(response, false);

			fcm.send({to: fcmDestination, data: {
				json: JSON.stringify({
					text: text,
					numbers: numbers
				})
			}}, function(error, fcmResponse) {
				if (error) handleResponse(response, false);
				else try {
					fcmResponse = JSON.parse(fcmResponse);

					if (typeof fcmResponse === 'object' && fcmResponse.message_id) {

						numbers.forEach(function(number) {
							if (!number.id) return;
							DELIVERY_STATUSES[number.id] = 'SENT';
						});

						handleResponse(response, true);
					}

					else handleResponse(response, false);

				} catch (exception) { handleResponse(response, false); }
			});

		}

		else if (operation === 'GETSTATUS') {
			var responseStr = '<?xml version="1.0" encoding="UTF-8"?><deliveryreport>';
			for (var c = 0; c < data.length; c++) {
				var messageId = data[c];
				var status = (DELIVERY_STATUSES.hasOwnProperty(messageId) ? DELIVERY_STATUSES[messageId] : 'NOT_DELIVERED');
				if (status === 'DELIVERED') delete DELIVERY_STATUSES[messageId];
				responseStr += `<message id="${messageId}" sentdate="xxxxx" donedate="xxxxx" status="${status}"/>`;
			}
			responseStr += '</deliveryreport>';
			return response.end(responseStr);
		}

		else response.end('NOTHING_TO_PROCESS');

	});
});


server.listen(Settings.portNumber);