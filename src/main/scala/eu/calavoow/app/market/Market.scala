package eu.calavoow.app.market

import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.api.Models._

import scalacache._
import memoization._
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.guava.GuavaCache
import scala.util.matching.Regex

object Market extends LazyLogging {
	implicit val scalaCache = ScalaCache(GuavaCache())

	def getRegions(@cacheKeyExclude auth: String): Map[String, NamedCrestLink[Region]] = memoize {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		logger.trace(s"Root fetched: $root")
		val regions = root.regions.followLink(oAuth)
		logger.trace(s"Regions fetched: $regions")

		regions.items.map {i ⇒ i.name → i} toMap
	}

	def getMarketOrders(regionName: String,
	                    itemTypeName: String,
	                    @cacheKeyExclude auth: String
		                   ): Option[(List[MarketOrders.Item], List[MarketOrders.Item])] = memoize(5 minutes) {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)

		val region = getRegions(auth).get(regionName).map(_.link.followLink(oAuth))
		logger.debug(region.toString)

		// Get the itemType link.
		val itemTypes = getAllItemTypes(auth)
		val itemTypeLink = itemTypes.find(_.name == itemTypeName).map(_.href)

		for (
			regionInst ← region;
			itemLink ← itemTypeLink
		) yield {
			// Get all pages of market orders.
			val buys = regionInst.marketBuyOrders.followLink(oAuth, Map("type" → itemLink)).authedIterable(oAuth).map(_.items).flatten.toList
			val sells = regionInst.marketSellOrders.followLink(oAuth, Map("type" → itemLink)).authedIterable(oAuth).map(_.items).flatten.toList
			(buys, sells)
		}
	}

	def getMarketHistory(regionName: String,
		                    itemTypeName: String,
	                    @cacheKeyExclude auth: String) : Option[MarketHistory] = memoize(1 day) {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)

		val region = getRegions(auth).get(regionName).map(_.link.href)
		logger.debug(region.toString)
		val marketIdRegex = """.*/(\d+)/.*""".r
		val marketId = region.flatMap(marketIdRegex.findFirstMatchIn) map (_.group(1))

		// Get the itemType link.
		val itemTypes = getAllItemTypes(auth)
		val itemTypeLink = itemTypes.find(_.name == itemTypeName).map(_.href)
		logger.debug(s"Item type link: $itemTypeLink")
		val itemTypeIdRegex = """.*/(\d+)/""".r
		val itemId = itemTypeLink.flatMap(itemTypeIdRegex.findFirstMatchIn) map (_.group(1))
		logger.debug(s"ItemID: $itemId")

		val marketHistory = for(
			mId ← marketId;
			iId ← itemId
		) yield {
			MarketHistory.fetch(marketID = mId.toInt, typeID = iId.toInt, auth = oAuth)
		}

		// Sort the history backwards in time.
		marketHistory.map { history ⇒ history.copy(items = history.items.sortBy(_.date)(Ordering[String].reverse))}
	}

	/**
	 * The turnover for the given avg buy and avg sell.
	 *
	 * Cost per bought item
	 * avgBuy * (1 + margin)
	 *
	 * Profit per sold item
	 * avgSell * (1 - margin - tax)
	 *
	 * @param avgBuy The average buy price to calculate with
	 * @param avgSell Average sell price
	 * @param volume The volume of the time period
	 * @param margin The broker order tax.
	 * @param tax Tax deducted from sell orders
	 * @return The flip margin for `volume` items.
	 */
	def turnOver(avgBuy: Double, avgSell: Double, volume: Long, margin: Double, tax: Double) : Double = {
		(avgSell - avgBuy - margin * (avgSell + avgBuy) + avgSell * tax) * volume
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
				logger.debug(s"sum: $volumeSum, for ($orderVolume, $price)")
				// Change to (volume, running sum of volume, price)
				(orderVolume, volumeSum + orderVolume, price)
			}).drop(1) // Drop the first element (0,0,0.0)
				.map { case r@(orderVolume, runningVolume, price) ⇒
				logger.debug(r.toString)
				// Weigh the order by volume, limited by the max `volume`
				if(runningVolume < volume) {
					orderVolume * price
				} else {
					// How much volume is there left that is less than `volume`?
					val volumeLeft = volume - (runningVolume - orderVolume)
					volumeLeft * price
				}
			} takeWhile { x ⇒
				logger.debug(x.toString)
				x >= 0
			} sum

			// Calculate the average weighted price
			summedWeightedPrice / volume
		}

		val weightedBuy = weightedPrice(buyOrders.sortBy(_.price)(Ordering[Double].reverse), volume)
		logger.trace(s"Weighted buy: $weightedBuy")
		val weightedSell = weightedPrice(sellOrders.sortBy(_.price), volume)
		logger.trace(s"Weighted sell: $weightedSell")
		(weightedBuy, weightedSell)
	}

	def getAllItemTypes(@cacheKeyExclude auth: String): List[UnImplementedNamedCrestLink] = memoize(2 days) {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		val itemTypesRoot = root.itemTypes.followLink(oAuth)

		itemTypesRoot.authedIterable(Some(auth)).map(_.items).flatten.toList
	}
}
