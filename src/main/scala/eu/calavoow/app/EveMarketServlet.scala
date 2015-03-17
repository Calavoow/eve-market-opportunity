package eu.calavoow.app

import api.Models.MarketOrders
import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.api.Login
import eu.calavoow.app.config.Config
import market.Market
import org.scalatra._
import spray.json._

class EveMarketServlet extends EveMarketOpportunityStack with ApiFormats with LazyLogging {

	get("/") {
		val config = Config.readApiConfig
		val loginLink = ""
		<html>
			<head>
				<title>Market Mapper Homepage</title>
				<script type="text/javascript" src="js/d3.v3.min.js" charset="utf-8"></script>
				<script type="text/javascript" src="js/cookie.js"></script>
				<script type="text/javascript" src="js/home.js"></script>
			</head>
			<body>
				<h1 style="display:none">
					<a id="login" data-clientid={config.clientId}>Please login</a>
				</h1>
			</body>
		</html>
	}

	get("/login") {
		<html>
			<head>
				<script type="text/javascript" src="js/d3.v3.min.js" charset="utf-8"></script>
				<script type="text/javascript" src="js/cookie.js"></script>
				<script type="text/javascript" src="js/login.js"></script>
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
		if( authToken.isDefined ) halt(400, "User already has a valid authentication token.")

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
		if( authToken.isDefined ) halt(400, "User already has a valid authentication token.")

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

		val pageContent = authCode match {
			case Some(auth) ⇒
				val regions = Market.getRegions(auth)
				for ( region ← regions.keys ) yield {
					<li>
						<a href={s"market/$region"}>
							{region}
						</a>
					</li>
				}
			case None ⇒ "No authentication code provided."
		}
		<html>
			<head>
				<script type="text/javascript" src="js/d3.v3.min.js" charset="utf-8"></script>
				<script type="text/javascript" src="js/cookie.js"></script>
				<script type="text/javascript" src="js/market.js"></script>
			</head>
			<body>
				<ul>
					{pageContent}
				</ul>
			</body>
		</html>
	}

	get("/market/:region") {
		val authCode = cookies.get("access_token")

		val pageContent = authCode match {
			case Some(auth) ⇒
				val regionName = params("region")
				Market.getMarketOrders(regionName, "Nocxium", auth)
					.getOrElse(halt(500, "Something went wrong fetching market orders"))

			case None ⇒ "No authentication code provided."
		}
		<html>
			<head>
				<script type="text/javascript" src="js/d3.v3.min.js" charset="utf-8"></script>
				<script type="text/javascript" src="js/cookie.js"></script>
				<script type="text/javascript" src="js/market.js"></script>
			</head>
			<body>
				{pageContent}
			</body>
		</html>
	}

	get("/marketData/:region") {
		contentType = formats("json")
		val oTax = params.get("tax")

		import eu.calavoow.app.api.CrestLink.CrestProtocol._
		case class CoreMarketInfo(avgBuy: Double, avgSell: Double, dailyTurnOver: Option[Double])
		implicit val outputFormat: JsonFormat[CoreMarketInfo] = jsonFormat3(CoreMarketInfo)

		val authCode = cookies.get("access_token")

		authCode match {
			case Some(auth) ⇒
				val regionName = params("region")
				val (buyOrders, sellOrders) = Market.getMarketOrders(regionName, "Nocxium", auth)
					.getOrElse(halt(500, "Something went wrong fetching market orders"))
				val marketHistory = Market.getMarketHistory(regionName, "Nocxium", auth)
					.getOrElse(halt(500, "Something went wrong fetching market history"))

				logger.trace(s"Market history head: ${marketHistory.items.headOption}")
				// Assuming |items| > 0
				val itemVolume = marketHistory.items.headOption.getOrElse(halt(500, "Market history was empty")).volume
				logger.trace(s"Item volume: $itemVolume")
				val item10Vol = (itemVolume / 10.0).ceil.toLong
				val (avgBuy, avgSell) = Market.avgPrice(buyOrders, sellOrders, item10Vol)
				logger.debug(s"Average buy/sell price: $avgBuy/$avgSell")
				val dailyTurnOver = oTax.map { tax ⇒ Market.turnOver(avgBuy, avgSell, item10Vol, tax.toDouble) }
				Ok(CoreMarketInfo(avgBuy, avgSell, dailyTurnOver).toJson.compactPrint)
			case None ⇒ halt(401, "The authentication token is not set.")
		}
	}
}
