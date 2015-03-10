"use strict";

window.onload = function() {
	var h = window.location.search.replace("?", "");;
	if(h) {
		var r = /code=([^&]+)&state=([^&]+)/;

		var matches = h.match(r);
		if(!matches[1] || !matches[2]) {
            alert("GET data incomplete")
            return;
        }

		var access_code = matches[1];
		var state = matches[2];

		var sessionCsrf = sessionStorage.getItem('csrf_token');
		if(!sessionCsrf) {
			gotoHomepage("Session timed out, retry logging in from the homepage.");
		} else if(state !== sessionCsrf) {
			gotoHomepage("The security token has been changed. Retry logging in from the homepage.");
		} else {
			var xhrRequest = d3.json("authCode");

			xhrRequest.post(access_code, function(error, data) {
					if(error) {
						alert(error.response);
					} else {
						console.log(data);
						var expected_keys = ['access_token','expires_in','refresh_token','token_type'];
						if(expected_keys.every(function(elem) { return data.hasOwnProperty(elem) })) {
							var expiryDate = new Date(((new Date()).getTime()) + data['expires_in']*1000);
							docCookies.setItem('access_token', data['access_token'], expiryDate);
							// The refresh token (presumably) has no expiry date.
							docCookies.setItem('refresh_token', data['refresh_token'], Infinity);
							docCookies.setItem('token_type', data['token_type'], expiryDate);
							window.location.replace("/");
						} else {
							alert('Unexpected data received.')
						}
					}
			});
		}
	} else {
		gotoHomepage("Please go to the homepage to login.");
	}

	function gotoHomepage(msg) {
		if(window.confirm(msg)){
			window.location.replace("/");
		}
	}
}