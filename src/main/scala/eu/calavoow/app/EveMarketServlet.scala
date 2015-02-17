package eu.calavoow.app

import java.util.Date

import eu.calavoow.app.api.Login.LoginParams
import eu.calavoow.app.api.Market
import org.scalatra._
import org.slf4j.LoggerFactory
import scalate.ScalateSupport
import api.Login

import scala.io.Source

class EveMarketServlet extends EveMarketOpportunityStack with CsrfTokenSupport {

	val logger = LoggerFactory.getLogger(getClass)

	get("/") {
		logger.debug(csrfKey)
		val loginUrl = Login.loginUrl(csrfToken).getOrElse(halt(500, "Reading the api.conf went wrong"))
		<html>
			<head>
				<script src="js/cookie.js"></script>
				<script src="js/home.js"></script>
			</head>
			<body>
				<h1>
					<a id="login" csrftoken={csrfToken} href={loginUrl}>Please login</a>
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

	post("/loginToken") {
		val oLoginParams = Login.LoginParams.unapply(params)
		oLoginParams match {
			case Some(loginParams) ⇒
				session.setAttribute("loginParams", loginParams)
				Ok
			case None ⇒
				halt(400, "Get paramaters are not correct")
		}
	}

	get("/market") {
		val ologinParams = Option(session.getAttribute("loginParams")).map(_.asInstanceOf[LoginParams])
		logger.info(s"User requesting region list: $ologinParams")

		ologinParams match {
			case None ⇒
				halt(401, "Your session has expired")
			case Some(loginParams) ⇒
				val regions = Market.getRegions(loginParams.accessToken)
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
		}
	}

	get("/market/:region") {
		val oLoginParams = Option(session.getAttribute("loginParams")).map(_.asInstanceOf[LoginParams])
		logger.info(s"User requesting region $oLoginParams")

		oLoginParams match {
			case None ⇒
				halt(401, "Your session has expired")
			case Some(loginParams) ⇒
				val regionName = params("region")
				val marketInfo = Market.getMarketOrders(regionName, "Nocxium", loginParams.accessToken)
					.getOrElse(halt(500, "Something went wrong fetching market orders"))
				<html>
					<body>
						{
						marketInfo
						}
					</body>
				</html>
		}
	}

}
