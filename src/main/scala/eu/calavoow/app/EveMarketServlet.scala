package eu.calavoow.app

import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.api.Login.LoginParams
import eu.calavoow.app.api.{Login, Market}
import eu.calavoow.app.config.Config
import org.scalatra._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.util.{Failure, Success, Try}

class EveMarketServlet extends EveMarketOpportunityStack with ApiFormats with LazyLogging {

	get("/") {
		val config = Config.readApiConfig
		val loginLink = ""
		<html>
			<head>
				<title>Market Mapper Homepage</title>
				<script src="js/d3.v3.min.js" charset="utf-8"></script>
				<script src="js/cookie.js"></script>
				<script src="js/home.js"></script>
			</head>
			<body>
				<h1 style="display:none">
					<a id="login" data-clientid={config.clientId} href="">Please login</a>
				</h1>
			</body>
		</html>
	}

	get("/login") {
		<html>
			<head>
				<script src="js/d3.v3.min.js" charset="utf-8"></script>
				<script src="js/cookie.js"></script>
				<script src="js/login.js"></script>
			</head>
			<body>
				Please wait, processing login information.
			</body>
		</html>
	}

	/**
	 * Expects in the message body only the access code or refresh token to be exchanged for the auth token.
	 */
	post("/authCode") {
		contentType = formats("json")

		val authToken = cookies.get("auth_token")
		if(authToken.isDefined) halt(400, "User already has a valid authentication token.")

		val oResult = request.body match {
			case "" ⇒
				halt(400, "No access code given.")
			case token ⇒
				Login.exchangeAccessCode(token)
		}

		oResult match {
			case Some(result) ⇒
				result
			case None ⇒
				halt(401, "Unable to exchange access code for auth token.")
		}
	}

	post("/refreshToken") {
		contentType = formats("json")

		val authToken = cookies.get("access_token")
		if(authToken.isDefined) halt(400, "User already has a valid authentication token.")

		val oResult = request.body match {
			case "" ⇒
				halt(400, "No refresh token given.")
			case token ⇒
				Login.exchangeRefreshToken(token)
		}

		oResult match {
			case Some(result) ⇒
				result
			case None ⇒
				halt(401, "Unable to exchange access code for auth token.")
		}
	}

	get("/market") {
		val authCode = cookies.get("access_token")

		authCode match {
			case Some(auth) ⇒
				val regions = Market.getRegions(auth)
				val regionsList = for(region ← regions) yield {
					<li><a href={s"market/${region.name}"}>{region.name}</a></li>
				}
				<html>
					<body>
						<ul>
							{regionsList}
						</ul>
					</body>
				</html>
			case None ⇒ halt(401, "No authentication code provided.")
		}
	}

	get("/market/:region") {
		val authCode = cookies.get("access_token")

		authCode match {
			case Some(auth) ⇒
				val regionName = params("region")
				val marketInfo = Market.getMarketOrders(regionName, "Nocxium", auth)
					.getOrElse(halt(500, "Something went wrong fetching market orders"))
				<html>
					<body>
						{
						marketInfo
						}
					</body>
				</html>
			case None ⇒ halt(401, "No authentication code provided.")
		}
	}

	def handleNoAuth : PartialFunction[Option[LoginParams], Unit] = {
		case Some(LoginParams(None,_)) ⇒
			halt(401, "The authentication token is not set.")
		case None ⇒
			halt(401, "Your session has expired")
	}

}
