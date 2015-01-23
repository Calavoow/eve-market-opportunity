package api

import config.Config.CrestAuthentication
import eu.calavoow.app.api.Map
import Map.CrestLink

object Market {
	val endpoint = "https://crest.eveonline.com/"

	def getMarketOrders(auth: CrestAuthentication) = {
		val rootCrestLink = CrestLink[Map.Root](endpoint)
		val root = rootCrestLink.followLink
	}

}
