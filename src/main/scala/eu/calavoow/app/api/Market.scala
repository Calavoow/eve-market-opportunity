package eu.calavoow.app.api

import eu.calavoow.app.api.Models.Root
import org.slf4j.LoggerFactory

object Market {
	val logger = LoggerFactory.getLogger(getClass)

	def getRegions(auth: String) = {
		val root = Root.fetch(auth)
		val regions = root.regions.followLink(auth)

		regions.items
	}

	def getMarketOrders(region: String, auth: String) = {
		val root = Root.fetch(auth)
		val regions = root.regions.followLink(auth)
		val region = regions.items.find(_.name == region).map(_.href.followLink(auth))

		val itemTypes = root.itemTypes.followLink(auth)
		val nocxiumLink = itemTypes.items.find(_.name == "Nocxium").map(_.href)

//		region.map(_.)
	}
}
