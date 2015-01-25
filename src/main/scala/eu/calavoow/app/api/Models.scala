package eu.calavoow.app.api

import scalaj.http.{HttpRequest, Http}


object Models {

	/**
	 * CrestLink contains a crest URL to follow, creating another Crest instance
	 * @param url The Crest URL to the next link
	 * @tparam T The type of CrestContainer to construct.
	 */
	case class CrestLink[T <: CrestContainer](url: String) {
		def followLink(auth: Option[String]): T = {
			//get
			val postRequest = Http(url).method("POST")
			// If the auth is set then add it as parameter.
			val authedRequest = auth.foldLeft(postRequest)((req : HttpRequest, authKey) ⇒ {
				req.param("Authentication", s"Bearer + $authKey")
			})

			val response = authedRequest.asString
			//json to object
			???
		}
	}

	sealed trait CrestContainer
	object Root {
		def apply : Root = {
			// The only "static" CREST URL.
			val endpoint = "https://crest.eveonline.com/"
			CrestLink[Root](endpoint).followLink
		}
	}
	case class Root(tokenDecode : CrestLink[TokenDecode], market : CrestLink[Market]) extends CrestContainer
	case class TokenDecode(test : String) extends CrestContainer
	case class Market(test : String) extends CrestContainer

}
