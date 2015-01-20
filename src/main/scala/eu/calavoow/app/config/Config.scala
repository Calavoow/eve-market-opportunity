package config

import java.io.Reader

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Config {
	val logger = LoggerFactory.getLogger(getClass)
	case class CrestAuthentication(clientId: String, secretKey: String)

	implicit class RichConfig(val underlying: com.typesafe.config.Config) extends AnyVal {
		def getOptional[T](path: String, f: (String ⇒ T)) = if(underlying.hasPath(path)) {
			Some(f(path))
		} else {
			None
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
