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
import concurrent.{ExecutionContext, Future}
import scala.async.Async.{async,await}
import scala.util.control.NonFatal

class EveMarketServlet extends EveMarketOpportunityStack with ApiFormats with LazyLogging with FutureSupport {

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
	post("/authCode") { new AsyncResult {
		override val is = {
			contentType = formats("json")

			val authToken = cookies.get("auth_token")
			if( authToken.isDefined ) halt(400, "User already has a valid authentication token.")

			val futResult = request.body match {
				case "" ⇒
					halt(400, "No access code given.")
				case token ⇒
					Login.exchangeAccessCode(token)
			}

			futResult.transform(identity, halt(401, "Unable to exchange access code for auth token."))
		}
	}}

	post("/refreshToken") { new AsyncResult {
		override val is = {
			contentType = formats("json")

			val authToken = cookies.get("access_token")
			if( authToken.isDefined ) halt(400, "User already has a valid authentication token.")

			val futResult = request.body match {
				case "" ⇒
					halt(400, "No refresh token given.")
				case token ⇒
					Login.exchangeRefreshToken(token)
			}
			futResult.transform(identity, halt(401, "Unable to exchange refresh token for auth token."))
		}
	}}

	get("/market") { new AsyncResult {
		override val is = async {
			val authCode = cookies.get("access_token")

			val pageContent = authCode match {
				case Some(auth) ⇒
					val regions = await(Market.getRegions(auth))
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
	}}

	get("/market/:region") { new AsyncResult {
		override val is = async {
			val authCode = cookies.get("access_token")
			val pageContent = authCode match {
				case Some(auth) ⇒
					val regionName = params("region")
					val marketOrders = Market.getMarketOrders(regionName, "Nocxium", auth)
					marketOrders.onFailure {
						case NonFatal(_) ⇒
							logger.error("Something went wrong fetching market orders.")
							halt(500, "Something went wrong fetching market orders")
					}
					await(marketOrders)
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
	}}

	get("/marketData/:region") { new AsyncResult {
		override val is = async {
			contentType = formats("json")
			val oTax = params.get("tax")
			val oBrokerTax = params.get("brokertax")
			val input = for(tax ← oTax; margin ← oBrokerTax) yield (margin.toDouble, tax.toDouble)
			val authCode = cookies.get("access_token")

			// Define some data form classes.
			import eu.calavoow.app.api.CrestLink.CrestProtocol._
			case class CoreMarketInfo(coreItemInfo: Iterable[CoreItemInfo])
			case class CoreItemInfo(itemType: String, avgBuy: Double, avgSell: Double, dailyTurnOver: Option[Double])
			implicit val coreItemInfoFormatter: JsonFormat[CoreItemInfo] = jsonFormat4(CoreItemInfo)
			implicit val outputFormat: JsonFormat[CoreMarketInfo] = jsonFormat1(CoreMarketInfo)

			authCode match {
				case Some(auth) ⇒
					val regionName = params("region")
					val futMarketData = Market.allMarketData(regionName, auth)
					futMarketData.onFailure {
						case NonFatal(e) ⇒ halt(500, "Something went wrong fetching market data.")
					}
					val allMarketData = await(futMarketData)

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
	}}

	get("/test/:region") {
		val regionName = params("region")
		val authCode = cookies("access_token")
		val allMarketHistory = Market.getAllMarketHistory(regionName, authCode)
		allMarketHistory
	}

	override protected implicit def executor: ExecutionContext = scala.concurrent.ExecutionContext.global
}
