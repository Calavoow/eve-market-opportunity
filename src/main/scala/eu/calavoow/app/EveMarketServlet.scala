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
			<body>
				<h1>
					<a href={loginUrl}>Please login</a>
				</h1>
			</body>
		</html>
	}

	get("/login") {
		<html>
			<head>
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
			.filter { loginParams ⇒
			loginParams.expiresOn.after(new Date())
		}

		ologinParams match {
			case None ⇒
				halt(403, "Your session has expired")
			case Some(loginParams) ⇒
				val root = Market.getMarketOrders(loginParams.accessToken)
				<html>
					<body>
						Your token:
						{session.getAttribute("loginParams")}<br/>
						Root:
						{root}
					</body>
				</html>
		}
	}

}
