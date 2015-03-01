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

		var obj = {
			"access_code": matches[1],
		};
		var state = matches[2];

		var sessionCsrf = sessionStorage.getItem('csrf_token');
		if(!sessionCsrf) {
			if(window.confirm("Session timed out, retry logging in from the homepage.")) {
                window.location.replace("/");
            } // Else do nothing
		} else if(state !== sessionCsrf) {
			alert("Your session has been hijacked by a third party.");
		} else {
			var xhrRequest = d3.xhr("accessCode")
				.header("Content-Type", "application/json")

			xhrRequest.post(JSON.stringify(obj), function(error, data) {
					if(error) {
						alert(error.response)
					} else {
						console.log(data)
					}
				})
		}
	} else {
		if(window.confirm("Please go to the homepage to login.")) {
			window.location.replace("/");
		} // Else do nothing
	}
}