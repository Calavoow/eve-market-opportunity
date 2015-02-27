package eu.calavoow.app

import eu.calavoow.app.api.Login.LoginParams
import eu.calavoow.app.api.Market
import eu.calavoow.app.config.Config
import org.scalatra._
import org.slf4j.LoggerFactory
import scalate.ScalateSupport
import api.Login

import scala.io.Source

class EveMarketServlet extends EveMarketOpportunityStack {

	val logger = LoggerFactory.getLogger(getClass)

	get("/") {
		val config = Config.readApiConfig
		<html>
			<head>
				<script src="js/cookie.js"></script>
				<script src="js/home.js"></script>
			</head>
			<body>
				<h1>
					<a id="login" data-clientId={config.clientId}>Please login</a>
				</h1>
			</body>
		</html>
	}

	get("/login") {
		<html>
			<head>
				<script src="js/cookie.js"></script>
				<script src="js/login.js"></script>
			</head>
			<body>
				Please wait, processing login information.
			</body>
		</html>
	}

	post("/accessCode") {
		val accessCode = params.get("access_code")
		val oResult = accessCode flatMap(Login.exchangeCode)
		oResult match {
			case Some(result) ⇒
				Ok(result)
			case None ⇒
				halt(401, "Unable to exchange access code for auth token.")
		}
	}

	post("/refreshToken") {
		val oLoginParams = Login.LoginParams.unapply(params)
		oLoginParams match {
			case Some(loginParams) ⇒
				session.setAttribute("loginParams", loginParams)
				Ok
			case None ⇒
				halt(400, "Get paramaters are incorrect")
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
