package eu.calavoow.app.api

import spray.json.JsonFormat

object Models {
	/**
	 * A CrestContainer is a class that contains links to followup pages and the information on the current crest page.
	 */
	sealed trait CrestContainer
	trait AuthedIterable[T <: AuthedIterable[T]] {
		def next: Option[CrestLink[T]]
		def authedIterable(auth: Option[String]) : Iterable[T] = authedIterable(auth, Map())
		def authedIterable(auth: Option[String], params: Map[String,String]) : Iterable[T] = new Iterable[T] {
			override def iterator = new Iterator[T] {
				var self : Option[T] = Some(AuthedIterable.this.asInstanceOf[T])
				override def hasNext = self.isDefined
				override def next() = {
					val res = self.get
					self = res.next.map(_.followLink(auth, params))
					res
				}
			}
		}
	}

	case class UnImplementedCrestLink(href: String) extends CrestContainer

	case class NamedCrestLink[T: JsonFormat](href: String, name: String) {
		lazy val link = CrestLink[T](href)
	}
	case class UnImplementedNamedCrestLink(href: String, name: String) extends CrestContainer

//	object NamedCrestLink {
//		def apply[T: JsonFormat](href: String, name: String) = new NamedCrestLink[T](href, name)
//	}
//	class NamedCrestLink[T: JsonFormat](override val href: String, val name: String) extends CrestLink[T](href)

	object Root {
		def fetch(auth: Option[String]): Root = {
			import CrestLink.CrestProtocol._
			// The only "static" CREST URL.
			val endpoint = "https://crest.eveonline.com/"
			CrestLink[Root](endpoint).followLink(auth)
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
	                  marketBuyOrders: CrestLink[MarketOrders],
	                  name: String,
	                  constellations: List[UnImplementedCrestLink],
	                  marketSellOrders: CrestLink[MarketOrders]) extends CrestContainer

	case class ItemTypes(totalCount_str: String,
	                     pageCount: Int,
	                     items: List[UnImplementedNamedCrestLink],
	                     next: Option[CrestLink[ItemTypes]],
	                     totalCount: Int,
	                     pageCount_str: String,
	                     previous: Option[CrestLink[ItemTypes]]) extends CrestContainer with AuthedIterable[ItemTypes]

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
		                price: Long,
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
		                       previous: Option[CrestLink[MarketOrders]]) extends CrestContainer with AuthedIterable[MarketOrders]


}
