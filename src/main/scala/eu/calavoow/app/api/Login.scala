package eu.calavoow.app.api

import java.util.Date

import config.Config
import org.joda.time.Seconds

import scala.io.{Codec, Source}

object Login {
	def loginUrl(csrfToken: String) : Option[String] = {
		val oathURL = "https://login.eveonline.com/oauth/authorize/"
		val redirectURL = "http://localhost:8080/login"
		Option(getClass.getResource("/api.conf"))
			.map(Source.fromURL)
			.flatMap { confF ⇒ Config.readApiConfig(confF.bufferedReader) }
			.map { auth ⇒
				s"$oathURL?" +
					s"response_type=token" +
					s"&client_id=${auth.clientId}" +
					s"&scope=" +
					s"&redirect_uri=$redirectURL" +
					s"state=$csrfToken"
			}
	}

	object LoginParams {
		def unapply(params: Map[String, String]) = {
			val token = params.get("access_token")
			val tType = params.get("token_type")
			val expiresIn = params.get("expires_in").map(_.toInt)
			for(x ← token;
				y ← tType;
				z ← expiresIn) yield {
				val date = new Date(System.currentTimeMillis() + z * 1000)
				LoginParams(x,y,date)
			}
		}
	}

	/**
	 * The returned GET parameters from an SSO redirect
	 * @param accessToken
	 * @param tokenType
	 * @param expiresOn
	 */
	case class LoginParams(accessToken: String, tokenType: String, expiresOn: Date)
}
