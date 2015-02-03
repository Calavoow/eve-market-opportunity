package eu.calavoow.app.api

import eu.calavoow.app.api.Models.Root
import org.slf4j.LoggerFactory

object Market {
	val logger = LoggerFactory.getLogger(getClass)

	def getMarketOrders(auth: String) = {
		val root = Root.fetch(auth)
		logger.info(root.toString)
		val marketPrices = root.regions.followLink(auth)
	}
}
