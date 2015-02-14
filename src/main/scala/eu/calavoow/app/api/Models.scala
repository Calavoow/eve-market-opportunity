package eu.calavoow.app.api

import java.net.SocketTimeoutException

import org.slf4j.LoggerFactory

import scalaj.http.{HttpRequest, Http}
import org.scalatra.Control
import spray.json._

object Models {

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
			implicit val regionsItemFormat: JsonFormat[Regions.Item] = lazyFormat(jsonFormat2(Regions.Item))

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
		def followLink(auth: Option[String], params : Map[String,String] = Map.empty): T = {
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
				jsonAst.convertTo[T]
			} catch {
				case timeout: SocketTimeoutException ⇒
					logger.warn(s"Timeout while requesting from EVE CREST: $timeout")
					new Control {}.halt(504, "Eve CREST API did not respond on time.")
			}
		}
	}

	/**
	 * A CrestContainer is a class that contains links to followup pages and the information on the current crest page.
	 */
	sealed trait CrestContainer

	case class UnImplementedCrestLink(href: String) extends CrestContainer

	object Root {
		def fetch(auth: String): Root = {
			import CrestLink.CrestProtocol._
			// The only "static" CREST URL.
			val endpoint = "https://crest.eveonline.com/"
			CrestLink[Root](endpoint).followLink(Some(auth))
		}

		case class Motd(dust: UnImplementedCrestLink,
		                eve: UnImplementedCrestLink,
		                server: UnImplementedCrestLink)

		case class UserCounts(dust: Double,
		                      dust_str: String,
		                      eve: Double,
		                      eve_str: String)

		case class Industry(facilities: UnImplementedCrestLink,
		                    specialities: UnImplementedCrestLink,
		                    teamsInAuction: UnImplementedCrestLink,
		                    systems: UnImplementedCrestLink,
		                    teams: UnImplementedCrestLink)

		case class Clients(dust: UnImplementedCrestLink,
		                   eve: UnImplementedCrestLink)
	}

	case class Root(crestEndpoint: UnImplementedCrestLink,
	                corporationRoles: UnImplementedCrestLink,
	                itemGroups: UnImplementedCrestLink,
	                channels: UnImplementedCrestLink,
	                corporations: UnImplementedCrestLink,
	                alliances: UnImplementedCrestLink,
	                itemTypes: CrestLink[ItemTypes],
	                decode: UnImplementedCrestLink,
	                battleTheatres: UnImplementedCrestLink,
	                marketPrices: UnImplementedCrestLink,
	                itemCategories: UnImplementedCrestLink,
	                regions: CrestLink[Regions],
	                marketGroups: UnImplementedCrestLink,
	                tournaments: UnImplementedCrestLink,
	                map: UnImplementedCrestLink,
	                wars: UnImplementedCrestLink,
	                incursions: UnImplementedCrestLink,
	                authEndpoint: UnImplementedCrestLink,
	                industry: Root.Industry,
	                clients: Root.Clients,
	                time: UnImplementedCrestLink,
	                marketTypes: UnImplementedCrestLink) extends CrestContainer

	object Regions {
		case class Item(href: CrestLink[Region], name: String)
	}

	case class Regions(totalCount_str: String,
	                   items: List[Regions.Item],
	                   pageCount: Double,
	                   pageCount_str: String,
	                   totalCount: Double) extends CrestContainer

	case class Region(description: String,
		                         marketBuyOrders: CrestLink[MarketOrders],
		                         name: String,
		                         constellations: List[UnImplementedCrestLink],
		                         marketSellOrders: CrestLink[MarketOrders]
		                         ) extends CrestContainer

	object ItemTypes {
		case class Item(href: UnImplementedCrestLink, name: String)
	}

	case class ItemTypes(totalCount_str: String,
		                         pageCount: Double,
		                         items: List[ItemTypes.Item],
		                         next: Option[CrestLink[ItemTypes]],
		                         totalCount: Double,
		                         pageCount_str: String,
		                         previous: Option[CrestLink[ItemTypes]]
		                         ) extends CrestContainer


	object MarketOrders {
		case class Reference(id_str: String,
			                   href: UnImplementedCrestLink,
			                   id: Double,
			                   name: String)

		case class Item(volume_str: String,
			                buy: Boolean,
			                issued: String,
			                price: Double,
			                volumeEntered: Double,
			                minVolume: Double,
			                volume: Double,
			                range: String,
			                href: UnImplementedCrestLink,
			                duration_str: String,
			                location: Reference,
			                duration: Double,
			                minVolume_str: String,
			                volumeEntered_str: String,
							`type`: Reference,
							id: Double,
							id_str: String)

	}

	case class MarketOrders(totalCount_str: String,
		                         items: List[Items],
		                         pageCount: Double,
		                         pageCount_str: String,
		                         totalCount: Double) extends CrestContainer


}
