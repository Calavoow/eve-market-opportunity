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
				<script src="http://d3js.org/d3.v3.min.js" charset="utf-8"></script>
				<script src="js/cookie.js"></script>
				<script src="js/home.js"></script>
			</head>
			<body>
				<h1>
					<a id="login" data-clientid={config.clientId} href="">Please login</a>
				</h1>
			</body>
		</html>
	}

	get("/login") {
		<html>
			<head>
				<script src="http://d3js.org/d3.v3.min.js" charset="utf-8"></script>
				<script src="js/cookie.js"></script>
				<script src="js/login.js"></script>
			</head>
			<body>
				Please wait, processing login information.
			</body>
		</html>
	}

	case class AccessCode(access_code: String)
	post("/accessCode") {
		implicit val accessCodeFormatter = jsonFormat1(AccessCode)
		logger.trace("Exchanging access code")
		contentType = formats("json")
		// Try to convert the body to an AccessCode
		val accessCode = Try(request.body.parseJson.convertTo[AccessCode].access_code)
		logger.trace("parsed {}", accessCode.toString)
		val oResult = accessCode map(Login.exchangeCode)
		oResult match {
			case Success(Some(result)) ⇒
				result
			case Success(None) ⇒
				halt(401, "Unable to exchange access code for auth token.")
			case Failure(fail) ⇒
				halt(400, "Provided arguments are wrong")
		}
	}

	post("/refreshToken") {
		val oLoginParams = Login.LoginParams.unapply(params)
		oLoginParams match {
			case Some(loginParams) ⇒
				session.setAttribute("loginParams", loginParams)
				Ok
			case None ⇒
				halt(400, "Get parameters are incorrect")
		}
	}

	get("/market") {
		val ologinParams = Option(session.getAttribute("loginParams")).map(_.asInstanceOf[LoginParams])
		logger.debug(s"User requesting region list: $ologinParams")


		ologinParams match {
			case Some(LoginParams(Some(accessToken), _)) ⇒
				val regions = Market.getRegions(accessToken)
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
			case x ⇒ handleNoAuth(x)
		}
	}

	get("/market/:region") {
		val oLoginParams = Option(session.getAttribute("loginParams")).map(_.asInstanceOf[LoginParams])
		logger.info(s"User requesting region $oLoginParams")


		oLoginParams match {
			case Some(LoginParams(Some(accessToken),_)) ⇒
				val regionName = params("region")
				val marketInfo = Market.getMarketOrders(regionName, "Nocxium", accessToken)
					.getOrElse(halt(500, "Something went wrong fetching market orders"))
				<html>
					<body>
						{
						marketInfo
						}
					</body>
				</html>
			case x ⇒ handleNoAuth(x)
		}
	}

	def handleNoAuth : PartialFunction[Option[LoginParams], Unit] = {
		case Some(LoginParams(None,_)) ⇒
			halt(401, "The authentication token is not set.")
		case None ⇒
			halt(401, "Your session has expired")
	}

}
