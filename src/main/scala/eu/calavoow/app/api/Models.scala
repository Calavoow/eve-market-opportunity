package eu.calavoow.app.api

import java.util.Date

import eu.calavoow.app.util.Util
import spray.json.JsonFormat

import scala.util.Try

import scala.util.Success

object Models {

	/**
	 * A CrestContainer is a class that contains links to followup pages and the information on the current crest page.
	 */
	sealed trait CrestContainer

	trait AuthedIterable[T <: AuthedIterable[T]] {
		def next: Option[CrestLink[T]]

		def authedIterable(auth: Option[String], retries: Int = 1, params: Map[String, String] = Map.empty): Iterable[T] =
			new Iterable[T] {
				override def iterator = new Iterator[T] {
					var self: Option[Try[T]] = Some(Success(AuthedIterable.this.asInstanceOf[T]))

					override def hasNext = !(self.isEmpty && self.get.isFailure)

					override def next() = {
						val res = self.get.get
						self = for(nxt  ← res.next) yield Util.retry(retries) {
							nxt.tryFollow(auth, params)
						}
						res
					}
				}
			}
	}

	case class UnImplementedCrestLink(href: String) extends CrestContainer

	/**
	 * To follow this crest link some construction is required.
	 *
	 * See the class methods to construct a normal CrestLink
	 * @param href The link to follow.
	 */
	case class UncompletedCrestLink(href: String) extends CrestContainer

	object NamedCrestLink{
		implicit class NamedCrestLink2CrestLink[T: JsonFormat](nlink: NamedCrestLink[T]) {
			def toCrestLink: CrestLink[T] = {
				nlink.link
			}
		}
	}
	case class NamedCrestLink[T: JsonFormat](href: String, name: String) {
		lazy val link = CrestLink[T](href)
	}

	case class UnImplementedNamedCrestLink(href: String, name: String) extends CrestContainer

	object Root {
		def fetch(auth: Option[String]): Root = {
			import CrestLink.CrestProtocol._
			// The only "static" CREST URL.
			val endpoint = "https://crest-tq.eveonline.com/"
			CrestLink[Root](endpoint).follow(auth)
		}

		case class Motd(dust: UnImplementedCrestLink,
		                eve: UnImplementedCrestLink,
		                server: UnImplementedCrestLink)

		case class UserCounts(dust: Int,
		                      dust_str: String,
		                      eve: Int,
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

	case class Regions(totalCount_str: String,
	                   items: List[NamedCrestLink[Region]],
	                   pageCount: Int,
	                   pageCount_str: String,
	                   totalCount: Int) extends CrestContainer

	case class Region(description: String,
	                  marketBuyOrders: UncompletedCrestLink,
	                  name: String,
	                  constellations: List[UnImplementedCrestLink],
	                  marketSellOrders: UncompletedCrestLink) extends CrestContainer {
		import CrestLink.CrestProtocol._
		def marketBuyLink(itemType: CrestLink[ItemType]) : CrestLinkParams[MarketOrders] = {
			new CrestLinkParams[MarketOrders](marketBuyOrders.href, Map("type" → itemType.href))
		}

		def marketSellLink(itemType: CrestLink[ItemType]): CrestLinkParams[MarketOrders] = {
			new CrestLinkParams[MarketOrders](marketSellOrders.href, Map("type" → itemType.href))
		}
	}

	case class ItemTypes(totalCount_str: String,
	                     pageCount: Int,
	                     items: List[NamedCrestLink[ItemType]],
	                     next: Option[CrestLink[ItemTypes]],
	                     totalCount: Int,
	                     pageCount_str: String,
	                     previous: Option[CrestLink[ItemTypes]]) extends CrestContainer with AuthedIterable[ItemTypes]

	/**
	 * TODO: Fill in this stub.
	 * @param description
	 */
	case class ItemType(description: String) extends CrestContainer

	object MarketOrders {

		/**
		 * A reference to an item type.
		 * @param id_str String of the id
		 * @param href The link is not implemented yet in the EVE CREST.
		 * @param id The id
		 * @param name The name of the itemtype.
		 */
		case class Reference(id_str: String,
		                     href: String,
		                     id: Int,
		                     name: String)

		/**
		 *
		 * @param href The link has not been implemented yet in EVE CREST.
		 */
		case class Item(volume_str: String,
		                buy: Boolean,
		                issued: String,
		                price: Double,
		                volumeEntered: Long,
		                minVolume: Long,
		                volume: Long,
		                range: String,
		                href: String,
		                duration_str: String,
		                location: Reference,
		                duration: Int,
		                minVolume_str: String,
		                volumeEntered_str: String,
		                `type`: Reference,
		                id: Long,
		                id_str: String)

	}

	case class MarketOrders(totalCount_str: String,
	                        items: List[MarketOrders.Item],
	                        pageCount: Int,
	                        pageCount_str: String,
	                        totalCount: Int,
	                        next: Option[CrestLink[MarketOrders]],
	                        previous: Option[CrestLink[MarketOrders]]) extends CrestContainer {
		/**
		 * Construct an iterable through the market orders.
		 *
		 * A parameter itemType is required to iterate through the market orders.
		 *
		 * TODO: Check if this is the case
		 * @param auth
		 * @param itemType
		 * @return
		 */
		def authedIterable(auth: Option[String], itemType: CrestLink[ItemType]) : Iterable[MarketOrders] = {
			new AuthedIterable[MarketOrders] {}.authedIterable(auth, Map("type" → itemType.href))
		}
	}

	object MarketHistory {
		case class Item(volume_str: String,
			                orderCount: Long,
			                lowPrice: Double,
			                highPrice: Double,
			                avgPrice: Double,
			                volume: Long,
			                orderCount_str: String,
			                date: String)

		def fetch(marketID: Int, typeID: Int, auth: Option[String]) = {
			import CrestLink.CrestProtocol._
			CrestLink[MarketHistory](s"https://crest-tq.eveonline.com/market/$marketID/types/$typeID/history/").follow(auth)
		}
	}

	case class MarketHistory(totalCount_str: String,
		                         items: List[MarketHistory.Item],
		                         pageCount: Int,
		                         pageCount_str: String,
		                         totalCount: Int) extends CrestContainer


}
