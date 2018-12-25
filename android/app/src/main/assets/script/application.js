var template = `

	<div class="wrapper">
		{{if !this.permissions}}
			Разрешите сервису отсылать SMS, для этого нажмите кнопку "Установить разрешения для SMS"
			которая находится внизу экрана, затем кликните "Разрешить" в появившемся диалоговом окне.
		{{else}}
			<div>
				{{if this.isSubscribed}}
					Прокси сервер включен, для выключения сервера, нажмите кнопку "Выключить прокси сервер" которая находится внизу экрана.
				{{else}}
					Прокси сервер выключен, для включения сервера, нажмите кнопку "Включить прокси сервер" которая находится внизу экрана.
				{{/if}}
			</div>
			<div>
				Используйте следующий код для подключения к прокси и отсылки сообщений: <span class="phoneToken">{{this.phoneToken}}</span>
			</div>
		{{/if}}
	</div>

	<div>
		{{if !this.permissions}}
			<div class="button requestPermissions">
				Установить разрешения для SMS
			</div>
		{{elseif this.isSubscribed}}
			<div class="button doUnsubscribe">
				Выключить прокси сервер
			</div>
		{{else}}
			<div class="button doSubscribe">
				Включить прокси сервер
			</div>
		{{/if}}
	</div>
`;

function updateUI(ret) {
	Histone(template).render(function(result) {
		document.body.innerHTML = result;
		if (typeof ret === 'function') ret();
	}, {
		permissions: android.checkPermissions(),
		phoneToken: android.getPhoneToken(),
		isSubscribed: android.getIsSubscribed()
	});
}

$(document).on('onSubscriptionChange onPermissionChange', updateUI);

$(document).on('click', '.doSubscribe', function() {
	$(this).addClass('disabled');
	android.doSubscribe();
});

$(document).on('click', '.doUnsubscribe', function() {
	$(this).addClass('disabled');
	android.doUnsubscribe();
});

$(document).on('click', '.requestPermissions', function() {
	$(this).addClass('disabled');
	android.grantPermissions();
});

updateUI(() => android.readyReady());