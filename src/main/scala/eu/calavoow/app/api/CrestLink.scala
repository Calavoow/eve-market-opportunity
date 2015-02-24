package eu.calavoow.app.api

import java.net.SocketTimeoutException

import eu.calavoow.app.api.Models._
import eu.calavoow.app.api.Models.NamedCrestLink
import org.scalatra.Control
import org.slf4j.LoggerFactory
import scalaj.http.Http
import spray.json._

import scalaj.http.HttpRequest

object CrestLink {

	/**
	 * Defines the JSON deserialisation protocols related to the Crest classes.
	 *
	 * Imported in CrestLink.followLink.
	 */
	object CrestProtocol extends DefaultJsonProtocol {
		implicit val unImplementedFormat: JsonFormat[UnImplementedCrestLink] = jsonFormat1(UnImplementedCrestLink)

		implicit val rootFormat: JsonFormat[Root] = lazyFormat(jsonFormat22(Root.apply))
		implicit val rootMotdFormat: JsonFormat[Root.Motd] = jsonFormat3(Root.Motd)
		implicit val rootUserCountsFormat: JsonFormat[Root.UserCounts] = jsonFormat4(Root.UserCounts)
		implicit val rootIndustryFormat: JsonFormat[Root.Industry] = jsonFormat5(Root.Industry)
		implicit val rootClientsFormat: JsonFormat[Root.Clients] = jsonFormat2(Root.Clients)

		implicit val regionsFormat: JsonFormat[Regions] = lazyFormat(jsonFormat5(Regions.apply))
		implicit val regionsCrestLinkFormat: JsonFormat[CrestLink[Regions]] = jsonFormat(CrestLink[Regions] _, "href")
		implicit val regionsItemFormat: JsonFormat[NamedCrestLink[Region]] = jsonFormat(NamedCrestLink[Region] _, "href", "name")

		implicit val regionFormat: JsonFormat[Region] = lazyFormat(jsonFormat5(Region.apply))
		implicit val regionCrestLinkFormat: JsonFormat[CrestLink[Region]] = jsonFormat(CrestLink[Region] _, "href")

		implicit val itemTypesFormat: JsonFormat[ItemTypes] = lazyFormat(jsonFormat7(ItemTypes.apply))
		implicit val itemTypesCrestLinkFormat: JsonFormat[CrestLink[ItemTypes]] = jsonFormat(CrestLink[ItemTypes] _, "href")
		implicit val itemTypesItemFormat: JsonFormat[ItemTypes.Item] = jsonFormat2(ItemTypes.Item)

		implicit val marketOrdersFormat: JsonFormat[MarketOrders] = lazyFormat(jsonFormat5(MarketOrders.apply))
		implicit val marketOrdersCrestLinkFormat: JsonFormat[CrestLink[MarketOrders]] = jsonFormat(CrestLink[MarketOrders] _, "href")
		implicit val marketOrdersLocationFormat: JsonFormat[MarketOrders.Reference] = jsonFormat4(MarketOrders.Reference)
		implicit val MarketOrdersItemsFormat: JsonFormat[MarketOrders.Item] = jsonFormat17(MarketOrders.Item)
	}

}

/**
 * CrestLink contains a crest URL to follow, creating another Crest instance
 * @param href The Crest URL to the next link
 * @tparam T The type of CrestContainer to construct.
 */
case class CrestLink[T: JsonFormat](href: String) {
	private val logger = LoggerFactory.getLogger(getClass)

	/**
	 * A convenience method to call followLink without construction an authentication Option.
	 * @param auth The authentication key.
	 * @return The constructed Crest class.
	 */
	def followLink(auth: String): T = followLink(Some(auth))

	/**
	 * Followlink executes a request to the CREST API to instantiate the linked Crest class T.
	 *
	 * On failure halts webpage construction with HTTP error code.
	 * @param auth The authentication key or None if unused.
	 * @param params Optional parameters to add to the request.
	 * @return The constructed Crest class.
	 */
	def followLink(auth: Option[String], params: Map[String, String] = Map.empty): T = {
		logger.trace(s"Fetching with $auth")
		//get
		val postRequest = Http(href).method("GET")

		val acceptRequest = postRequest.header("Accept", "application/json, charset=utf-8")
		// If the auth is set then add it as parameter.
		val authedRequest = auth.foldLeft(acceptRequest)((req: HttpRequest, authKey) ⇒ {
			req.header("Authorization", s"Bearer $authKey")
		})

		val finalRequest = authedRequest.params(params)

		logger.trace(authedRequest.toString)

		try {
			val response = finalRequest.asString
			if (response.isError) {
				new Control {}.halt(response.code, response.body)
			}

			//json to crest object using implicit protocols.
			import CrestLink.CrestProtocol._
			val jsonAst = response.body.parseJson
			logger.trace(jsonAst.prettyPrint)
			jsonAst.convertTo[T]
		} catch {
			case timeout: SocketTimeoutException ⇒
				logger.warn(s"Timeout while requesting from EVE CREST: $timeout")
				new Control {}.halt(504, "Eve CREST API did not respond on time.")
		}
	}
}