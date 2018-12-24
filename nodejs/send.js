var FCM = require('fcm-push'),
	Settings = require('./settings.json'),
	fcm = new FCM(Settings.serverToken),
	fcmDestination = '/topics/' + Settings.phoneToken;


fcm.send({to: fcmDestination, data: {
	phone: process.argv[2],
	text: process.argv[3]
}}, function(error, response) {
	if (error) console.info(error);
	console.info(response);
});