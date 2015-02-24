package eu.calavoow.app.api

import CrestLink.CrestProtocol._
import spray.json.JsonFormat

object Models {
	/**
	 * A CrestContainer is a class that contains links to followup pages and the information on the current crest page.
	 */
	sealed trait CrestContainer

	case class UnImplementedCrestLink(href: String) extends CrestContainer

	case class NamedCrestLink[T: JsonFormat](href: String, name: String) {
		lazy val link = CrestLink[T](href)
	}

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

	case class Regions(totalCount_str: String,
	                   items: List[NamedCrestLink[Region]],
	                   pageCount: Double,
	                   pageCount_str: String,
	                   totalCount: Double) extends CrestContainer

	case class Region(description: String,
	                  marketBuyOrders: CrestLink[MarketOrders],
	                  name: String,
	                  constellations: List[UnImplementedCrestLink],
	                  marketSellOrders: CrestLink[MarketOrders]) extends CrestContainer

	object ItemTypes {

		case class Item(href: UnImplementedCrestLink, name: String)

	}

	case class ItemTypes(totalCount_str: String,
	                     pageCount: Double,
	                     items: List[ItemTypes.Item],
	                     next: Option[CrestLink[ItemTypes]],
	                     totalCount: Double,
	                     pageCount_str: String,
	                     previous: Option[CrestLink[ItemTypes]]) extends CrestContainer

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
	                        items: List[MarketOrders.Item],
	                        pageCount: Double,
	                        pageCount_str: String,
	                        totalCount: Double) extends CrestContainer


}
