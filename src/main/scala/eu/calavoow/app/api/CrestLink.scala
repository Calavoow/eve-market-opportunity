package eu.calavoow.app.api

import java.net.SocketTimeoutException

import scala.concurrent
import scala.util.{Try,Failure,Success}
import com.typesafe.scalalogging.LazyLogging
import eu.calavoow.app.api.CrestLink.CrestCommunicationException
import eu.calavoow.app.api.Models._
import org.scalatra.Control
import spray.json._

import scalaj.http.{Http, HttpRequest}

object CrestLink {
	case class CrestCommunicationException(errorCode: Int, msg: String) extends RuntimeException(msg)

	/**
	 * Defines the JSON deserialisation protocols related to the Crest classes.
	 */
	object CrestProtocol extends DefaultJsonProtocol {
		implicit val unImplementedFormat: JsonFormat[UnImplementedCrestLink] = jsonFormat1(UnImplementedCrestLink)
		implicit val unCompletedFormat: JsonFormat[UncompletedCrestLink] = jsonFormat1(UncompletedCrestLink)
		implicit val unImplementedNamedFormat: JsonFormat[UnImplementedNamedCrestLink] = jsonFormat2(UnImplementedNamedCrestLink)

		implicit val rootFormat: JsonFormat[Root] = lazyFormat(jsonFormat22(Root.apply))
		implicit val rootMotdFormat: JsonFormat[Root.Motd] = jsonFormat3(Root.Motd)
		implicit val rootUserCountsFormat: JsonFormat[Root.UserCounts] = jsonFormat4(Root.UserCounts)
		implicit val rootIndustryFormat: JsonFormat[Root.Industry] = jsonFormat2(Root.Industry)
		implicit val rootClientsFormat: JsonFormat[Root.Clients] = jsonFormat2(Root.Clients)

		implicit val regionsFormat: JsonFormat[Regions] = lazyFormat(jsonFormat5(Regions.apply))
		implicit val regionsCrestLinkFormat: JsonFormat[CrestLink[Regions]] = jsonFormat(CrestLink[Regions] _, "href")
		implicit val regionsItemFormat: JsonFormat[NamedCrestLink[Region]] = jsonFormat(NamedCrestLink[Region] _, "href", "name")

		implicit val regionFormat: JsonFormat[Region] = lazyFormat(jsonFormat5(Region.apply))
		implicit val regionCrestLinkFormat: JsonFormat[CrestLink[Region]] = jsonFormat(CrestLink[Region] _, "href")

		implicit val itemTypesFormat: JsonFormat[ItemTypes] = lazyFormat(jsonFormat7(ItemTypes.apply))
		implicit val itemTypesCrestLinkFormat: JsonFormat[CrestLink[ItemTypes]] = jsonFormat(CrestLink[ItemTypes] _, "href")

		implicit val itemTypeFormat: JsonFormat[ItemType] = lazyFormat(jsonFormat1(ItemType.apply))
		implicit val itemTypeCrestLinkFormat: JsonFormat[NamedCrestLink[ItemType]] = jsonFormat(NamedCrestLink[ItemType] _, "href", "name")

		implicit val marketOrdersFormat: JsonFormat[MarketOrders] = lazyFormat(jsonFormat7(MarketOrders.apply))
		implicit val marketOrdersCrestLinkFormat: JsonFormat[CrestLink[MarketOrders]] = jsonFormat(CrestLink[MarketOrders] _, "href")
		implicit val marketOrdersLocationFormat: JsonFormat[MarketOrders.Reference] = jsonFormat4(MarketOrders.Reference)
		implicit val MarketOrdersItemsFormat: JsonFormat[MarketOrders.Item] = jsonFormat17(MarketOrders.Item)

		implicit val marketHistoryFormat: JsonFormat[MarketHistory] = lazyFormat(jsonFormat5(MarketHistory.apply))
		implicit val marketHistoryCrestLinkFormat: JsonFormat[CrestLink[MarketHistory]] = jsonFormat(CrestLink[MarketHistory] _, "href")
		implicit val marketHistoryItemsFormat: JsonFormat[MarketHistory.Item] = jsonFormat8(MarketHistory.Item)
	}

}

/**
 * CrestLink contains a crest URL to follow, creating another Crest instance
 * @param href The Crest URL to the next link
 * @tparam T The type of CrestContainer to construct.
 */
case class CrestLink[T: JsonFormat](href: String) extends LazyLogging {
	/**
	 * Followlink executes a request to the CREST API to instantiate the linked Crest class T.
	 *
	 * On failure halts webpage construction with HTTP error code.
	 * @param auth The authentication key or None if unused.
	 * @param params Optional parameters to add to the request. Using this should *not* be necessary!
	 * @return The constructed Crest class.
	 */
	def follow(auth: Option[String], params: Map[String, String] = Map.empty): T = {
		logger.trace(s"Fetching with {}", auth)
		//get
		val getRequest = Http(href).method("GET")

		val acceptRequest = getRequest.header("Accept", "application/json, charset=utf-8")
		// If the auth is set then add it as parameter.
		val authedRequest = auth.foldLeft(acceptRequest)((req: HttpRequest, authKey) ⇒ {
			req.header("Authorization", s"Bearer $authKey")
		})

		val finalRequest = authedRequest.params(params)

		logger.trace(authedRequest.toString)

		try {
			val response = finalRequest.asString
			if( response.isError ) {
				logger.error(s"Error following link: ${response.code}\n${response.body}")
				new Control {}.halt(502, "EVE CREST returned an error")
			}

			//json to crest object using implicit protocols.
			val jsonAst = response.body.parseJson
			jsonAst.convertTo[T]
		} catch {
			case timeout: SocketTimeoutException ⇒
				logger.warn(s"Timeout while requesting from EVE CREST: $timeout")
				new Control {}.halt(504, "Eve CREST API did not respond on time.")
			case deserializationE: DeserializationException ⇒
				logger.error(deserializationE.toString)
				throw deserializationE
		}
	}

	/**
	 * TryFollowLinkg executes a request to the CREST API to instantiate the linked Crest class T.
	 *
	 * On failure return a Failure with the Throwable that caused the failure.
	 * @param auth The authentication key or None if unused.
	 * @param params Optional parameters to add to the request. Using this should *not* be necessary!
	 * @return A Try of the constructed Crest class.
	 */
	def tryFollow(auth: Option[String], params: Map[String, String] = Map.empty): Try[T] = {
		logger.trace(s"Fetching with {}", auth)
		//get
		val getRequest = Http(href).method("GET")

		val acceptRequest = getRequest.header("Accept", "application/json, charset=utf-8")
		// If the auth is set then add it as parameter.
		val authedRequest = auth.foldLeft(acceptRequest)((req: HttpRequest, authKey) ⇒ {
			req.header("Authorization", s"Bearer $authKey")
		})

		val finalRequest = authedRequest.params(params)

		logger.trace(authedRequest.toString)
		val tryResponse = Try(finalRequest.asString)
		tryResponse.flatMap { response ⇒
			if( response.isError ) {
				Failure(new CrestCommunicationException(response.code, response.body))
			} else {
				//json to crest object using implicit protocols.
				val jsonAst = Try(response.body.parseJson)
				logger.trace(jsonAst.map(_.prettyPrint).toString)
				jsonAst.flatMap { ast ⇒
					Try(ast.convertTo[T])
				}
			}
		}
	}
}

class CrestLinkParams[T: JsonFormat](href: String, params: Map[String,String]) extends CrestLink[T](href) {
	def follow(auth: Option[String]) : T = follow(auth, params)
	def tryFollow(auth: Option[String]): Try[T] = tryFollow(auth, params)
}