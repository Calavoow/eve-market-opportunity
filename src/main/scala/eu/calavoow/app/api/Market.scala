package eu.calavoow.app.api

import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.api.Models._
import org.slf4j.LoggerFactory

import scala.collection.mutable
//import scalacache._
//import memoization._
//import scalacache.guava.GuavaCache
import scala.language.postfixOps

object Market extends LazyLogging {
//	implicit val scalaCache = ScalaCache(GuavaCache())

	def getRegions(auth: String) = {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		val regions = root.regions.followLink(oAuth)

		regions.items
	}

	def getMarketOrders(regionName: String,
	                    itemTypeName: String,
	                    auth: String
		                   ): Option[(MarketOrders, MarketOrders)] = {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		val regions = root.regions.followLink(oAuth)
		logger.debug(regions.toString)
		val region = regions.items.find(_.name == regionName).map(_.link.followLink(auth))
		logger.debug(region.toString)

		// Get the itemType url.
		val itemTypes = getAllItemTypes(auth)
		val itemTypeLink = itemTypes.find(_.name == itemTypeName).map(_.href)

		for(
			regionInst ← region;
			itemLink ← itemTypeLink
		) yield {
			(regionInst.marketBuyOrders.followLink(oAuth, Map("type" → itemLink)),
				regionInst.marketSellOrders.followLink(oAuth, Map("type" → itemLink)))
		}
	}

	def dailyTurnOver(buyOrders: List[MarketOrders.Item], sellOrders: List[MarketOrders.Item], dailyVolume: Double, tax: Double) = {
		val (avgBuy, avgSell) = avgPrice(buyOrders, sellOrders, dailyVolume)
		dailyVolume*(avgBuy*(1-tax) - avgSell*(1+tax))
	}

	/**
	 * Calculates the average price of the given volume for the buy and sell orders.
	 * @param buyOrders The list of buy orders for the item
	 * @param sellOrders The list of sell orders for the item
	 * @param volume The volume which has to be bought to calculate the average.
	 * @return
	 */
	def avgPrice(buyOrders: List[MarketOrders.Item], sellOrders: List[MarketOrders.Item], volume: Double) : (Double, Double) = {
		// Lowest buy orders reaching volume
		var volumeFulfilled = 0.0d
		val buyOrdered = buyOrders.sortBy(_.price).takeWhile((order) ⇒ {
			if(volumeFulfilled > volume) {
				false
			} else {
				volumeFulfilled += order.volume
				true
			}
		})

		// Highest sell orders, that fulfill the required volume
		volumeFulfilled = 0
		val sellOrdered = sellOrders.sortBy(_.price)(Ordering.Double.reverse).takeWhile((order) ⇒ {
			if(volumeFulfilled > volume) {
				false
			} else {
				volumeFulfilled += order.volume
				true
			}
		})

		/**
		 * Calculate the price to buy the first 10% of the market items.
		 * @param buyOrder A sorted list of market orders, ordered by best price first.
		 * @return The price to buy the volume in items
		 */
		def weightedPrice(buyOrder: List[MarketOrders.Item]) : Double = {
			var volumeLeft = volume
			buyOrder.foldLeft(0.0d)((accum, buyOrder) ⇒ {
				val volume = Math.min(buyOrder.volume, volumeLeft)
				logger.debug(s"Volume: $volume")
				volumeLeft -= volume
				accum + volume * buyOrder.price
			})
		}

		(weightedPrice(buyOrdered), weightedPrice(sellOrdered))
	}

	def getAllItemTypes(auth: String) : List[UnImplementedNamedCrestLink] = {
		val oAuth = Some(auth)
		val root = Root.fetch(oAuth)
		val itemTypesRoot = root.itemTypes.followLink(oAuth)

		// Iterate trough all itemTypes pages and add them to the itemTypes list.
		val itemTypes = mutable.ListBuffer[UnImplementedNamedCrestLink]()
		itemTypes ++= itemTypesRoot.items
		var currentItemTypes = itemTypesRoot
		while(currentItemTypes.next.isDefined) {
			currentItemTypes = currentItemTypes.next.get.followLink(oAuth)
			itemTypes ++= currentItemTypes.items
		}
		itemTypes.toList // Return the immutable list.
	}
}
