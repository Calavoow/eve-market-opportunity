package eu.calavoow.app.api



object Map {

	/**
	 * CrestLink contains a crest URL to follow, creating another Crest instance
	 * @param url The Creast URL to the next link
	 * @tparam T The type of CrestContainer to construct.
	 */
	case class CrestLink[T <: CrestContainer](url: String) {
		def followLink: T = {
			//get
			//json to object
			???
		}
	}

	sealed trait CrestContainer
	case class Root(tokenDecode : CrestLink[TokenDecode], market : CrestLink[Market]) extends CrestContainer
	case class TokenDecode(test : String) extends CrestContainer
	case class Market(test : String) extends CrestContainer

}
