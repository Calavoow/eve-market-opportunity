package eu.calavoow.app.api

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

	def exchangeCode(accessCode: String) = {
		val tokenEndpoint= "https://login.eveonline.com/oauth/token"
		val config = Config.readApiConfig
		val request = Http(tokenEndpoint)
			.postForm(Seq("grant_type" → "authorization_code", "code" → accessCode))
			.auth(config.clientId,config.secretKey)
		logger.trace(s"AccessCode Request, headers : ${request.headers}\n params ${request.params}")

		val result = request.asString
		if(result.isSuccess) {
			Some(result.body)
		} else {
			logger.info(s"Exchanging the EVE access code went wrong: $result")
			None
		}
	}

	object LoginParams {
		def unapply(params: Map[String, String]) : Option[LoginParams] = {
			val token = params.get("access_token")
			val refreshToken = params.get("refresh_token")
//			val expiresIn = params.get("expires_in").map(_.toInt)
			for(y ← refreshToken) yield {
//				val date = new Date(System.currentTimeMillis() + z * 1000)
				LoginParams(token,y)
			}
		}
	}

	case class LoginParams(accessToken: Option[String], refreshToken: String)
}
