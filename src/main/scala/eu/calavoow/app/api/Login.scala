package eu.calavoow.app.api

import config.Config

import scala.io.{Codec, Source}

object Login {
	val oathUrl = "https://login.eveonline.com/oauth/authorize/"
	val redirectUri = "http://localhost:8080/login"

	def loginUrl : Option[String] = {
		Option(getClass.getResource("/api.conf"))
			.map(Source.fromURL)
			.flatMap { confF ⇒ Config.readApiConfig(confF.bufferedReader) }
			.map { auth ⇒
				s"$oathUrl?" +
					s"response_type=token" +
					s"&client_id=${auth.clientId}" +
					s"&scope=" +
					s"&redirect_uri=$redirectUri"
			}
	}

	object LoginParams {
		def unapply(params: Map[String, String]) = {
			val token = params.get("access_token")
			val tType = params.get("token_type")
			val expiresIn = params.get("expires_in").map(_.toInt)
			for(x ← token;
				y ← tType;
				z ← expiresIn) yield LoginParams(x,y,z)
		}
	}
	case class LoginParams(accessToken: String, tokenType: String, expiresIn: Int)
}
