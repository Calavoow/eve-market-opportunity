package eu.calavoow.app.api

import eu.calavoow.app.api.Models.Root

object Market {
	def getMarketOrders(auth: String) = {
		val root = Root.fetch(auth)
	}
}
