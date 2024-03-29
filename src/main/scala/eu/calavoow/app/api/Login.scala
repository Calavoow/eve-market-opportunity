package eu.calavoow.app.api

import java.net.SocketTimeoutException

import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.config.Config

import scalaj.http.Http

object Login extends LazyLogging {

	def loginUrl(csrfToken: String) = {
		val oathURL = "https://login.eveonline.com/oauth/authorize/"
		val redirectURL = "http://localhost:8080/login"
		val config = Config.readApiConfig
		s"$oathURL?" +
			s"response_type=code" +
			s"&client_id=${config.clientId}" +
			s"&scope=publicData" +
			s"&redirect_uri=$redirectURL" +
			s"&state=$csrfToken"
	}

	def exchangeAccessCode(accessCode: String) = exchangeCode(accessCode, "code", "authorization_code")

	def exchangeRefreshToken(token: String) = exchangeCode(token, "refresh_token", "refresh_token")

	def exchangeCode(accessCode: String, codeParam: String, grantType: String) = {
		val tokenEndpoint = "https://login.eveonline.com/oauth/token"
		val config = Config.readApiConfig
		val request = Http(tokenEndpoint)
			.postForm(Seq("grant_type" → grantType, codeParam → accessCode))
			.auth(config.clientId, config.secretKey)
			.timeout(5000, 5000)
		logger.trace(s"AccessCode Request, headers : ${request.headers}\n params ${request.params}")

		try {
			val result = request.asString
			if( result.isSuccess ) {
				Some(result.body)
			} else {
				logger.info(s"Exchanging the EVE access code went wrong: $result")
				None
			}
		} catch {
			case timeout: SocketTimeoutException ⇒
				logger.warn(s"Timeout while exchanging accessCode $accessCode: $timeout")
				None
		}
	}
}
