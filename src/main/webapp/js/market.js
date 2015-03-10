"use strict";

window.onload = function() {
	var access_token = docCookies.getItem("access_token");
	var refresh_token = docCookies.getItem("refresh_token");

	if(access_token){
		// Do nothing
	} else if(refresh_token) {
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
						docCookies.setItem('refresh_token', data['refresh_token'], Infinity);
						docCookies.setItem('token_type', data['token_type'], expiryDate);
						location.reload(); // Token refreshed, reload page with proper cookies set.
					} else {
						alert('Unexpected data received.')
					}
				}
		});
	} else {
		if(window.confirm("You are not authenticated, please log in on the homepage.")){
			window.location.replace("/");
		}
	}
}