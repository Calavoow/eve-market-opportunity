package eu.calavoow.app.api

import org.slf4j.LoggerFactory

import scalaj.http.{HttpRequest, Http}
import org.scalatra.Control
import spray.json._

object Models {

	object CrestLink {

		object CrestProtocol extends DefaultJsonProtocol {
			implicit val unImplementedFormat: JsonFormat[UnImplementedCrestLink] = jsonFormat1(UnImplementedCrestLink)

			implicit val rootFormat: JsonFormat[Root] = lazyFormat(jsonFormat22(Root.apply))
			implicit val rootMotdFormat: JsonFormat[Root.Motd] = jsonFormat3(Root.Motd)
			implicit val rootUserCountsFormat: JsonFormat[Root.UserCounts] = jsonFormat4(Root.UserCounts)
			implicit val rootIndustryFormat: JsonFormat[Root.Industry] = jsonFormat5(Root.Industry)
			implicit val rootClientsFormat: JsonFormat[Root.Clients] = jsonFormat2(Root.Clients)

			implicit val regionsFormat: JsonFormat[Regions] = lazyFormat(jsonFormat5(Regions.apply))
			implicit val regionsCrestLinkFormat: JsonFormat[CrestLink[Regions]] = jsonFormat(CrestLink[Regions] _, "href")
			implicit val regionsItemFormat: JsonFormat[Regions.Item] = jsonFormat2(Regions.Item)
		}

	}

	/**
	 * CrestLink contains a crest URL to follow, creating another Crest instance
	 * @param href The Crest URL to the next link
	 * @tparam T The type of CrestContainer to construct.
	 */
	case class CrestLink[T: JsonFormat](href: String) {
		val logger = LoggerFactory.getLogger(getClass)

		def followLink(auth: String): T = followLink(Some(auth))

		def followLink(auth: Option[String]): T = {
			logger.info(s"Fetching with $auth")
			//get
			val postRequest = Http(href).method("GET")

			val acceptRequest = postRequest.header("Accept", "application/json, charset=utf-8")
			// If the auth is set then add it as parameter.
			val authedRequest = auth.foldLeft(acceptRequest)((req: HttpRequest, authKey) ⇒ {
				req.header("Authorization", s"Bearer $authKey")
			})

			logger.info(authedRequest.toString)
			logger.info(s"Headers: ${authedRequest.headers.toString}")


			val response = authedRequest.asString
			if (response.isError) {
				new Control {}.halt(response.code, response.body)
			}

			//json to object
			import CrestLink.CrestProtocol._
			val jsonAst = response.body.parseJson
			println(jsonAst.prettyPrint)
			val converted = jsonAst.convertTo[T]
			println(converted)
			converted
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
	                itemTypes: UnImplementedCrestLink,
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
		case class Item(href: String, name: String)
	}

	case class Regions(totalCount_str: String,
	                   items: List[Regions.Item],
	                   pageCount: Double,
	                   pageCount_str: String,
	                   totalCount: Double)

}
