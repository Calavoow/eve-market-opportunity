package eu.calavoow.app.api

import eu.calavoow.app.config.Config
import org.slf4j.LoggerFactory
import scala.util.{Failure, Success}
import scalaj.http.Http

import scala.io.{Codec, Source}

object Login {
	val logger = LoggerFactory.getLogger(getClass)
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
			.method("POST")
			.auth(config.clientId,config.secretKey)
			.param("grant_type","authorization_code")
			.param("code", accessCode)

		val result = request.asString
		if(result.isSuccess) {
			Some(result.body)
		} else {
			logger.info("Exchanging the EVE access code went wrong", result.toString)
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
