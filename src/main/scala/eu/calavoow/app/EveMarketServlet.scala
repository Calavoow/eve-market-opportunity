package eu.calavoow.app

import java.io.PrintWriter

import api.Models.MarketOrders
import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.api.Login
import eu.calavoow.app.config.Config
import market.Market
import org.fusesource.scalate.util.Resource
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
		val oBrokerTax = params.get("brokertax")
		val input = for(tax ← oTax; margin ← oBrokerTax) yield (margin.toDouble, tax.toDouble)

		import eu.calavoow.app.api.CrestLink.CrestProtocol._
		case class CoreMarketInfo(coreItemInfo: Iterable[CoreItemInfo])
		case class CoreItemInfo(itemType: String, avgBuy: Double, avgSell: Double, dailyTurnOver: Option[Double])
		implicit val coreItemInfoFormatter: JsonFormat[CoreItemInfo] = jsonFormat4(CoreItemInfo)
		implicit val outputFormat: JsonFormat[CoreMarketInfo] = jsonFormat1(CoreMarketInfo)

		val authCode = cookies.get("access_token")

		authCode match {
			case Some(auth) ⇒
				val regionName = params("region")
				val allMarketHistory = Market.getAllMarketHistory(regionName, auth)
				val definedMarketTypes = allMarketHistory.filter(_._2.isDefined).map(_._1)
				val allMarketOrders = Market.getMarketOrders(regionName, definedMarketTypes, auth)
					.getOrElse(halt(500, "Something went wrong fetching market orders"))

				// A more descriptive container of the required data.
				case class MarketData(item: String,
				                      buyOrders: List[MarketOrders.Item],
				                      sellOrders: List[MarketOrders.Item],
				                      volume : Long)

				// merge both market orders and market history data
				val allMarketData = for (
					itemType ← definedMarketTypes
				) yield {
					// Note that it is assumed that there exist entries for each item type.
					val marketOrders = allMarketOrders(itemType.name)
					val oMarketHistory = allMarketHistory.get(itemType).flatten
					val volume = oMarketHistory.filter(_.items.size > 0)
						.map(_.items.maxBy(_.date).volume).getOrElse {
						logger.warn(s"History of item was empty: ${marketOrders._1}")
						0L
					}
					MarketData(itemType.name, marketOrders._1, marketOrders._2, volume)
				}

				val itemInfos = for ( marketData ← allMarketData ) yield {
					val item10Vol = (marketData.volume / 10.0).ceil.toLong
					val (avgBuy, avgSell) = Market.avgPrice(marketData.buyOrders, marketData.sellOrders, item10Vol)
					logger.debug(s"Average buy/sell price: $avgBuy/$avgSell")
					// If there is Some input, use it to calculate the daily turnover.
					val dailyTurnOver = input.map { case (brokerTax, tax) ⇒
						Market.turnOver(avgBuy, avgSell, item10Vol, brokerTax, tax)
					}
					CoreItemInfo(marketData.item, avgBuy, avgSell, dailyTurnOver)
				}
				val sortedInfos = itemInfos.toSeq.sortBy { info ⇒
					info.dailyTurnOver.getOrElse(0.0d)
				}
				try {
					val marketSortedFile = Resource.fromFile("marketSorted.csv")
					val writer = new PrintWriter(marketSortedFile.writer)
					for(info ← sortedInfos) {
						writer.println(s"${info.itemType},${info.avgBuy},${info.avgSell},${info.dailyTurnOver}")
					}
				} catch {
					case e: RuntimeException ⇒ logger.error(e.getMessage)
				}
				Ok(CoreMarketInfo(sortedInfos).toJson.compactPrint)
			case None ⇒ halt(401, "The authentication token is not set.")
		}
	}

	get("/test/:region") {
		val regionName = params("region")
		val authCode = cookies("access_token")
		val allMarketHistory = Market.getAllMarketHistory(regionName, authCode)
		allMarketHistory
	}
}
