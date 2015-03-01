"use strict";

window.onload = function() {
	var access_token = docCookies.getItem("access_token");
	var refresh_token = docCookies.getItem("refresh_token");
	var client_id = document.getElementById("login").dataset.clientid;

	if(access_token) {
	} else if (refresh_token) {
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

		document.getElementById("login").setAttribute("href", total_href)
	}


	// Generate an RFC4122 version 4 UUID
	function uuidGen() {
		return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
			var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
			return v.toString(16);
		});
	}
}