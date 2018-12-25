var FS = require('fs'),
	Path = require('path'),
	Request = require('request'),
	Settings = require('./settings.json');

FS.createReadStream(Path.resolve(__dirname, process.argv[2])).pipe(Request.post({
	url: `http://127.0.0.1:${Settings.portNumber}`,
	headers: {'content-type': 'application/octet-stream'}
}, function(error, response, body) {
	console.info(body)
}));