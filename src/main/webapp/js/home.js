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
		docCookies.setItem("csrf_token", csrf_token);
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
//	if(access_token && token_type) {
//	    var obj = {
//	        "access_token": access_token,
//	        "token_type": token_type
//	    };
//	    sendData(obj, csrfToken)
//	}

//	//Src: https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Forms/Sending_forms_through_JavaScript
//    function sendData(data, csrfToken) {
//        console.log(data);
//        var XHR = new XMLHttpRequest();
//        var urlEncodedData = "";
//        var urlEncodedDataPairs = [];
//        var name;
//
//        // We turn the data object into an array of URL encoded key value pairs.
//        for(name in data) {
//            urlEncodedDataPairs.push(encodeURIComponent(name) + '=' + encodeURIComponent(data[name]));
//        }
//
//        // We combine the pairs into a single string and replace all encoded spaces to
//        // the plus character to match the behaviour of the web browser form submit.
//        urlEncodedData = urlEncodedDataPairs.join('&').replace(/%20/g, '+');
//
//        // We define what will happen if the data is successfully sent
//        XHR.addEventListener('load', function(event) {
//            if(event.currentTarget.status != 200) {
//                alert('Unsuccessfully sent data, please retry logging in.');
//            } else {
//                window.location.replace('market');
//            }
//        });
//
//        // We define what will happen in case of error
//        XHR.addEventListener('error', function(event) {
//            alert('Oups! Something goes wrong.');
//        });
//
//        // We setup our request
//        XHR.open('POST', 'loginToken');
//
//        // We add the required HTTP header to handle a form data POST request
//        XHR.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
//
//        // (CUSTOM) Set CSRF token
//        XHR.setRequestHeader('X-CSRF-TOKEN', csrfToken);
//
//        // And finally, We send our data.
//        XHR.send(urlEncodedData);
//    }
}