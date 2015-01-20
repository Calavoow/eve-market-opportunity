package api

import config.Config.CrestAuthentication

object Market {
	val endpoint = "https://crest.eveonline.com/"

	def getMarketOrders(auth: CrestAuthentication) = {
//		val res = Http(endpoint + "market/prices/").asString
//		println(res)
	}

}
