"use strict";

window.onload = function() {
	var tokenServer = "https://login.eveonline.com/oauth/token"
	var h = window.location.hash;
	if(h) {
		var r = /code=([^&]+)&state=([^&]+)/;
		var matches = h.match(r);
		var obj = {
			"code": matches[1],
		};
		var state = matches[2];

		sendData(obj, state);
	} else {
		alert("Please go to the homepage to login.");
	}

	//Src: https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Forms/Sending_forms_through_JavaScript
	function sendData(data, csrfToken) {
		console.log(data);
		var XHR = new XMLHttpRequest();
		var urlEncodedData = "";
		var urlEncodedDataPairs = [];
		var name;

		// We turn the data object into an array of URL encoded key value pairs.
		for(name in data) {
			urlEncodedDataPairs.push(encodeURIComponent(name) + '=' + encodeURIComponent(data[name]));
		}

		// We combine the pairs into a single string and replace all encoded spaces to
		// the plus character to match the behaviour of the web browser form submit.
		urlEncodedData = urlEncodedDataPairs.join('&').replace(/%20/g, '+');

		// We define what will happen if the data is successfully sent
		XHR.addEventListener('load', function(event) {
			if(event.currentTarget.status != 200) {
				alert('Unsuccessfully sent data, please retry logging in.');
			} else {
				docCookies.setItem("access_token", data["access_token"], expiresIn);
				docCookies.setItem("token_type", data["token_type"], expiresIn);
				window.location.replace('market');
			}
		});

		// We define what will happen in case of error
		XHR.addEventListener('error', function(event) {
			alert('Oups! Something goes wrong.');
		});

		// We setup our request
		XHR.open('POST', 'loginToken');

		// We add the required HTTP header to handle a form data POST request
		XHR.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');

		// (CUSTOM) Set CSRF token
		XHR.setRequestHeader('X-CSRF-TOKEN', csrfToken);

		// And finally, We send our data.
		XHR.send(urlEncodedData);
	}
}