import eu.calavoow.app.api.Models.MarketOrders
import org.fusesource.scalate.util.Resource
import spray.json.pimpString

import concurrent._
import duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import spray.json._

val string =
	"""
	  |{"id_str": "123", "href": "http://asdasd, "id": 123, "name": "Hi"}
	""".stripMargin

import eu.calavoow.app.api.CrestLink.CrestProtocol._
val converted = Future {
	val parsed = string.parseJson
	parsed.convertTo[MarketOrders.Reference]
}

println("Before")
Await.ready(converted, 1 second)
println("after")

val y : Try[Int] = Success(1)
val z : Try[Int] = y.map {x ⇒
	throw new Exception("Hi")
}

val a : Future[Int] = Future { 123456 }
val b : Future[Int] = a.map { x ⇒
	throw new Exception("Exception from future")
}

a.onComplete(println)
Await.ready(b, 1 second)
Await.ready(a, 1 second)
println("Still running")