package eu.calavoow.app.config

import java.io.Reader

import com.typesafe.config.ConfigFactory
import eu.calavoow.app.api.Login.LoginParams._
import org.scalatra.Control
import org.slf4j.LoggerFactory

import scala.io.Source

object Config {
	val logger = LoggerFactory.getLogger(getClass)
	case class CrestAuthentication(clientId: String, secretKey: String)

	private implicit class RichConfig(val underlying: com.typesafe.config.Config) {
		def getOptional[T](path: String, f: (String ⇒ T)) = if(underlying.hasPath(path)) {
			Some(f(path))
		} else {
			None
		}
	}

	/**
	 * Read the CREST API Configuration using the default path.
	 * @return
	 */
	lazy val readApiConfig : CrestAuthentication = {
		Option(getClass.getResource("/api.conf"))
			.map(Source.fromURL)
			.flatMap {
			confF ⇒ Config.readApiConfig(confF.bufferedReader())
		} getOrElse {
			new Control {}.halt(500,
				"""Unable to read api config.
				  |Create an api.conf file in src/main/resources/api according to the following template:
				  |```
				  |api {
				  |	clientId="xxxx"
				  |	secretKey="xxxXXxxxx"
				  |}
				  |```
				""".stripMargin)
		}
	}

	def readApiConfig(location: Reader) : Option[CrestAuthentication] = {
		val config = ConfigFactory.parseReader(location)
		val keyOption = config.getOptional("api.clientId", config.getString)
		keyOption match {
			case None ⇒
				logger.error("You did not provide a clientId in the api.conf")
			case _ ⇒
		}
		val verifCodeOption = config.getOptional("api.secretKey", config.getString)
		verifCodeOption match {
			case None ⇒
				logger.error("You did not provide a valid secretKey in the api.conf")
			case _ ⇒
		}
		for(key ← keyOption; verif ← verifCodeOption) yield { CrestAuthentication(key, verif) }
	}
}
