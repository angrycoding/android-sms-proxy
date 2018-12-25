var FS = require('fs'),
	Request = require('request'),
	Settings = require('./settings.json');


FS.readFile('soap.xml', 'UTF-8', function(error, xml) {
	Request.post({
		url: `http://localhost:${Settings.portNumber}`,
		form: {XML: xml}
	}, function(error, response, body) {
		console.info(body)
	});
});