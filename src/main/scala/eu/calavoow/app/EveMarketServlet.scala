package eu.calavoow.app

import org.scalatra._
import scalate.ScalateSupport
import api.Login

import scala.io.Source

class EveMarketServlet extends EveMarketOpportunityStack {

  get("/") {
    val loginUrl = Login.loginUrl.getOrElse(halt(500, "Reading the api.conf went wrong"))

    <html>
      <body>
        <h1><a href={loginUrl}>Please login</a></h1>
      </body>
    </html>
  }

  get("/login") {
    val oLoginParams = Login.LoginParams.unapply(params)
    val loginParams = oLoginParams match {
      case Some(x) ⇒ x
      case None ⇒ halt(400, "Get paramaters are not correct")
    }
  }
}
