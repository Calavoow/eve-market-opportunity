package eu.calavoow.app

import org.scalatra._
import scalate.ScalateSupport
import api.Login

import scala.io.Source

class EveMarketServlet extends EveMarketOpportunityStack with CsrfTokenSupport {

  get("/") {
    val loginUrl = Login.loginUrl(csrfToken).getOrElse(halt(500, "Reading the api.conf went wrong"))
    <html>
      <body>
        <h1><a href={loginUrl}>Please login</a></h1>
      </body>
    </html>
  }

  get("/login") {
    val oLoginParams = Login.LoginParams.unapply(params)
    oLoginParams match {
      case Some(loginParams) ⇒
        session.setAttribute("loginParams", loginParams)
        redirect("market")
      case None ⇒
        halt(400, "Get paramaters are not correct")
    }
  }

  get("/market") {
    val loginParams = Option(session.getAttribute("loginParams"))

    <html>
      <body>
        Your token: {session.getAttribute("loginParams")}
      </body>
    </html>
  }
}
