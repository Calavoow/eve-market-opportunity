package eu.calavoow.app.market

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.api.Models._
import eu.calavoow.app.api.CrestLink
import eu.calavoow.app.util.Util
import eu.calavoow.app.util.Util.SkipException
import org.scalatra.Control

import concurrent._
import scala.util.control.NonFatal
import scalacache._
import memoization._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Try,Failure,Success}
import scalacache.guava.GuavaCache
import scala.util.matching.Regex
import scala.async.Async.{async,await}

object Market extends LazyLogging {
	implicit val scalaCache = ScalaCache(GuavaCache())
	implicit val threadPool = scala.concurrent.ExecutionContext.global

	def getRegions(@cacheKeyExclude auth: String): Future[Map[String, NamedCrestLink[Region]]] = memoize {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth) // Throw exception if it goes wrong.
		logger.trace(s"Root fetched: $root")
		val regions = root.flatMap(_.regions.follow(oAuth))
		logger.trace(s"Regions fetched: $regions")

		regions.map(_.items.map {i ⇒ i.name → i} toMap)
	}

	def getRegion(regionName: String, auth: String) : Future[CrestLink[Region]] = {
		val regions = getRegions(auth)
		regions.map(_.get(regionName).map(_.link).get)
	}

	def getMarketOrders(regionName: String,
	                    itemTypes: Iterable[NamedCrestLink[ItemType]],
	                    @cacheKeyExclude auth: String)
	: Future[Map[String, (List[MarketOrders.Item], List[MarketOrders.Item])]]
	= {
		async {
			val oAuth = Some(auth)
			val region = await(getRegion(regionName, auth).flatMap(_.follow(oAuth)))

			val atomicCounter = new AtomicInteger(0)
			val totalItemTypes = itemTypes.size
			val res = itemTypes.map { itemType ⇒
				val res = collectMarketOrders(region, itemType.link, oAuth)
				val count = atomicCounter.incrementAndGet()
				logger.debug(s"Pulled $count/$totalItemTypes")
				res.map {
					itemType.name → _
				}
			}
			await(Future.sequence(res).map(_.toMap))
		}
	}

	def getMarketOrders(regionName: String,
	                    itemTypeName: String,
	                    @cacheKeyExclude auth: String
		                   ): Future[(List[MarketOrders.Item], List[MarketOrders.Item])] = memoize(5 minutes) {
		async {
			val oAuth = Some(auth)
			val region = await(getRegion(regionName, auth).flatMap(_.follow(oAuth)))

			// Get the itemType link.
			val itemTypes = await(getAllItemTypes(auth))
			val itemTypeLink = itemTypes.find(_.name == itemTypeName)

			// For the found region and the item type, collect the market orders.
			val marketOrders = for (
				itemLink ← itemTypeLink
			) yield {
				collectMarketOrders(region, itemLink.link, oAuth)
			}
			await(marketOrders.getOrElse(Future.successful((Nil,Nil))))
		}
	}

	private def collectMarketOrders(region: Region,
	                                itemTypeLink: CrestLink[ItemType],
	                                @cacheKeyExclude oAuth: Option[String])
		: Future[(List[MarketOrders.Item], List[MarketOrders.Item])] = {
		async {
			// The number of times to retry a failed CREST request.
			val retries = 3
			// Try to get the first page of buy orders, retrying `retries` times.
			val futBuy = Util.retryFuture(retries) {
				region.marketBuyLink(itemTypeLink).follow(oAuth)
			}
			val futSell = Util.retryFuture(retries) {
				region.marketSellLink(itemTypeLink).follow(oAuth)
			}

			def allOrders(init: MarketOrders) = {
				// Take the first buy order, and collect all following buyorders.
				// If the first buy order failed, at least log it.
				val traversableBuys = init.authedIterator(itemTypeLink)(oAuth,retries).map(_.map(_.items))
				Future.sequence(traversableBuys).map(_.toList.flatten)
			}

			val allBuy = allOrders(await(futBuy))
			val allSell = allOrders(await(futSell))

			// Log errors
			allBuy.failed.foreach { throwable ⇒
				logger.info(s"Unable to fetch a buy, error msg: ${throwable.getMessage}")
			}
			allSell.failed.foreach { throwable ⇒
				logger.info(s"Unable to fetch a buy, error msg: ${throwable.getMessage}")
			}

			(await(allBuy), await(allSell))
		}
	}

	def getAllMarketHistory(regionName: String, auth: String)
	: Future[Map[NamedCrestLink[ItemType], Option[MarketHistory]]] = {
		async {
			val itemTypes = await(getAllItemTypes(auth))
			val atomicCounter = new AtomicInteger(0)
			val nrItemTypes = itemTypes.size
			val res = for(itemType ← itemTypes) yield {
				async {
					val res = await(getMarketHistory(regionName, itemType.name, auth))
					val count = atomicCounter.incrementAndGet()
					logger.debug(s"History $count/$nrItemTypes")
					itemType → res
				}
			}
			await(Future.sequence(res)).toMap
		}
	}

	def getMarketHistory(regionName: String,
		                 itemTypeName: String,
	                     @cacheKeyExclude auth: String) : Future[Option[MarketHistory]] = memoize(1 day) {
		async {
			val oAuth = Some(auth)

			val regionHref = await(getRegion(regionName, auth).map(_.href))
			val marketIdRegex = """.*/(\d+)/.*""".r
			val marketId = marketIdRegex.findFirstMatchIn(regionHref) map (_.group(1))

			// Get the itemType link.
			val itemTypes = await(getAllItemTypes(auth))
			val itemTypeLink = itemTypes.find(_.name == itemTypeName).map(_.href)
			val itemTypeIdRegex = """.*/(\d+)/""".r
			val itemId = itemTypeLink.flatMap(itemTypeIdRegex.findFirstMatchIn) map (_.group(1))

			val marketHistory = for (
				mId ← marketId;
				iId ← itemId
			) yield {
				Util.retryFuture(3) {
					val res = MarketHistory.fetch(marketID = mId.toInt, typeID = iId.toInt, auth = oAuth)
					res.failed.foreach(failure ⇒ logger.info(s"Failed fetching market history: $failure"))
					res
				}
			}

			val simplifiedHistory = marketHistory match {
				case Some(history) ⇒ history
				case None ⇒ Future.failed(new NoSuchElementException("No market or itemid found."))
			}

			// Sort the history backwards in time.
			await(simplifiedHistory).map { history ⇒ history.copy(items = history.items.sortBy(_.date)(Ordering[String].reverse))}
		}
	}

	case class MarketData(item: String,
	                      buyOrders: List[MarketOrders.Item],
	                      sellOrders: List[MarketOrders.Item],
	                      volume : Long)
	def allMarketData(regionName: String, auth: String): Future[Iterable[MarketData]] = {
		async {
			val allMarketHistory = await(Market.getAllMarketHistory(regionName, auth))
			val definedMarketTypes = allMarketHistory.filter(_._2.isDefined).map(_._1)
			val allMarketOrders = await(Market.getMarketOrders(regionName, definedMarketTypes, auth))

			// A more descriptive container of the required data.
			case class MarketData(item: String,
			                      buyOrders: List[MarketOrders.Item],
			                      sellOrders: List[MarketOrders.Item],
			                      volume : Long)

			// merge both market orders and market history data
			for (
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
				Market.MarketData(itemType.name, marketOrders._1, marketOrders._2, volume)
			}
		}
	}

	/**
	 * The turnover for the given avg buy and avg sell.
	 *
	 * Cost per bought item
	 * avgBuy * (1 + brokerTax)
	 *
	 * Profit per sold item
	 * avgSell * (1 - brokerTax - tax)
	 *
	 * @param avgBuy The average buy price to calculate with
	 * @param avgSell Average sell price
	 * @param volume The volume of the time period
	 * @param brokerTax The broker order tax.
	 * @param tax Tax deducted from sell orders
	 * @return The flip margin for `volume` items.
	 */
	def turnOver(avgBuy: Double, avgSell: Double, volume: Long, brokerTax: Double, tax: Double) : Double = {
		(avgSell - avgBuy - brokerTax * (avgSell + avgBuy) - avgSell * tax) * volume
	}

	/**
	 * Calculates the average price of the given volume for the buy and sell orders.
	 * @param buyOrders The list of buy orders for the item
	 * @param sellOrders The list of sell orders for the item
	 * @param volume The volume which has to be bought to calculate the average.
	 * @return
	 */
	def avgPrice(buyOrders: List[MarketOrders.Item], sellOrders: List[MarketOrders.Item], volume: Long): (Double, Double) = {
		/**
		 * Calculate the price to buy the first `volume` of the market items.
		 * @param marketOrders A sorted list of market orders, ordered by best price first.
		 * @return The price to buy the volume in items
		 */
		def weightedPrice(marketOrders: List[MarketOrders.Item], volume: Long): Double = {
			// Use a stream to lazily calculate up to the required elements.
			def summedWeightedPrice = marketOrders.toStream.map({ order ⇒
				// Map to (volume, price)
				(order.volume, order.price)
			}).scanLeft((0L,0L,0.0d))({ case ((_, volumeSum, _), (orderVolume, price)) ⇒
				// Change to (volume, running sum of volume, price)
				(orderVolume, volumeSum + orderVolume, price)
			}).drop(1) // Drop the first element (0,0,0.0)
				.map { case r@(orderVolume, runningVolume, price) ⇒
				// Weigh the order by volume, limited by the max `volume`
				if(runningVolume < volume) {
					orderVolume * price
				} else {
					// How much volume is there left that is less than `volume`?
					val volumeLeft = volume - (runningVolume - orderVolume)
					volumeLeft * price
				}
			} takeWhile { x ⇒
				x >= 0
			} sum

			// Calculate the average weighted price
			summedWeightedPrice / volume
		}

		// Order buy orders descending (highest first)
		val weightedBuy = weightedPrice(buyOrders.sortBy(_.price)(Ordering[Double].reverse), volume)
		val weightedSell = weightedPrice(sellOrders.sortBy(_.price), volume)
		logger.trace(s"Weighted buy/sell: $weightedBuy / $weightedSell")
		(weightedBuy, weightedSell)
	}

	def getAllItemTypes(@cacheKeyExclude auth: String): Future[List[NamedCrestLink[ItemType]]] = memoize(2 days) {
		async {
			val oAuth = Some(auth)
			val root = await(Root.fetch(oAuth))
			val itemTypesRoot = await(root.itemTypes.follow(oAuth))

			val itemsList = Future.sequence(itemTypesRoot.authedIterator(oAuth).map(_.map(_.items)).toList)
			await(itemsList).flatten
		}
	}
}
