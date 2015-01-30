package eu.calavoow.app.api

import scalaj.http.{HttpRequest, Http}
import org.scalatra.Control
import spray.json._

object Models {

	object CrestLink {
		object CrestProtocol extends DefaultJsonProtocol {
			implicit def crestLinkFormat[T <: CrestContainer]: RootJsonFormat[CrestLink[T]] = jsonFormat1(CrestLink[T])
			implicit val rootFormat : RootJsonFormat[Root] = jsonFormat1(Root.apply)
			implicit val marketFormat : RootJsonFormat[MarketPrices] = jsonFormat1(MarketPrices)
		}
	}

	/**
	 * CrestLink contains a crest URL to follow, creating another Crest instance
	 * @param href The Crest URL to the next link
	 * @tparam T The type of CrestContainer to construct.
	 */
	case class CrestLink[T <: CrestContainer](href: String) {
		def followLink(auth: Option[String]): T = {
			//get
			val postRequest = Http(href).method("GET")

			val acceptRequest = postRequest.header("Accept", "application/json, charset=utf-8")
			// If the auth is set then add it as parameter.
			val authedRequest = auth.foldLeft(acceptRequest)((req : HttpRequest, authKey) ⇒ {
				req.param("Authentication", s"Bearer + $authKey")
			})


			val response = authedRequest.asString
			if(response.isError) {
				new Control {}.halt(response.code, response.body)
			}
			println(response.body)


			//json to object
			import CrestLink.CrestProtocol._
			val jsonAst = response.body.parseJson
			println(jsonAst.prettyPrint)
			val converted = jsonAst.convertTo[T]
			println(converted)
			converted
		}
	}

	sealed trait CrestContainer
	object Root {
		def fetch(auth: String) : Root = {
			// The only "static" CREST URL.
			val endpoint = "https://crest.eveonline.com/"
			CrestLink[Root](endpoint).followLink(Some(auth))
		}
	}
	case class Root(marketPrices : CrestLink[MarketPrices]) extends CrestContainer
	case class MarketPrices(test : String) extends CrestContainer

}
