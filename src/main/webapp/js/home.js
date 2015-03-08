"use strict";

window.onload = function() {
	var access_token = docCookies.getItem("access_token");
	var refresh_token = docCookies.getItem("refresh_token");
	var client_id = document.getElementById("login").dataset.clientid;

	if(access_token) {
		window.location.replace("/market");
	} else if (refresh_token) {
		var xhrRequest = d3.json("refreshToken");

		xhrRequest.post(refresh_token, function(error, data) {
				if(error) {
					alert(error.response);
				} else {
					console.log(data);
					var expected_keys = ['access_token','expires_in','refresh_token','token_type'];
					if(expected_keys.every(function(elem) { return data.hasOwnProperty(elem) })) {
						var expiryDate = new Date(((new Date()).getTime()) + data['expires_in']*1000);
						docCookies.setItem('access_token', data['access_token'], expiryDate);
						// The refresh token (presumably) has no expiry date.
						docCookies.setItem('refresh_token', data['access_token'], Infinity);
						docCookies.setItem('token_type', data['token_type'], expiryDate);
						window.onload();
					} else {
						alert('Unexpected data received.')
					}
				}
		});
	} else {
		var login_page = "https://login.eveonline.com/oauth/authorize/"
		var csrf_token = uuidGen();
		sessionStorage.setItem('csrf_token', csrf_token);
		var total_href = login_page
			+ "?response_type=code"
			+ "&redirect_uri=http://localhost:8080/login"
			+ "&client_id=" + client_id
			+ "&scope=publicData"
			+ "&state=" + csrf_token;

		var login = document.getElementById("login");
		login.setAttribute("href", total_href);
		login.parentNode.setAttribute("style","");
	}


	// Generate an RFC4122 version 4 UUID
	function uuidGen() {
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
			var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
			return v.toString(16);
		});
	}
}