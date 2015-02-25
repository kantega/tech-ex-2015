package techex

import javax.servlet._
import javax.servlet.annotation.WebListener
import javax.servlet.http.{HttpServlet, HttpServletRequest}

import com.typesafe.config.{ConfigFactory, ConfigValueFactory, ConfigObject}
import org.http4s.server.HttpService
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.servlet.Http4sServlet
import techex.cases.{updateStream, startup}
import techex.data.slack
import techex.domain.{Alert, Good, Notification, Slack}
import techex.web.{WebsocketServiceServlet, Http4sWebsocket}
import scala.collection.JavaConversions._

@WebListener
class Bootstrap extends ServletContextListener {

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    println("Starting up app")


    /*BlazeBuilder.bindHttp(8090)
      .mountService(HttpService(updateStream.wsApi), "/stream")
      .run.awaitShutdown()
    */

    val ctx =
      sce.getServletContext

    val registration =
      ctx.addServlet("example", new InitingServlet())

    registration.addMapping("/*")
    registration.setAsyncSupported(true)
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {
    println("Shutting down app")
    slack.sendMessage("Server shutting down").run
  }
}

class InitingServlet extends HttpServlet {

  var wrapped  : Option[Http4sServlet]           = None
  var wsWrapped: Option[WebsocketServiceServlet] = None

  override def destroy(): Unit = {
    wrapped.foreach(_.destroy())
  }

  override def getServletConfig: ServletConfig = {
    wrapped.orNull
  }

  override def init(config: ServletConfig): Unit = {

    val dbName =
      config.getInitParameter("db.type")

    val username =
      config.getInitParameter("db.username")

    val pw =
      config.getInitParameter("db.password")

    val cfgMap =
      Map("db.type" -> dbName, "db.username" -> username, "db.password" -> pw)

    val cfg = ConfigFactory.load().withFallback(ConfigValueFactory.fromMap(cfgMap))

    val services =
      startup.setup(cfg)
        .onFinish(maybeErr =>
        if (maybeErr.isDefined)
          slack.sendMessage("Server failed to start: " + maybeErr.get.getMessage, Alert)
        else
          slack.sendMessage("Server started", Good)
        ).run

    wrapped = Some(new Http4sServlet(services._1))
    wsWrapped = Some(new WebsocketServiceServlet(services._2))
    wrapped.foreach(_.init(config))
    wsWrapped.foreach(_.init(config))
  }

  override def getServletInfo: String = {
    "Wrapper around " + wrapped.map(_.getServletInfo).getOrElse(" unknown servlet ")
  }

  override def service(req: ServletRequest, res: ServletResponse): Unit = {
    if (req.asInstanceOf[HttpServletRequest].getPathInfo startsWith "/ws")
      wsWrapped.foreach(_.service(req, res))
    else
      wrapped.foreach(_.service(req, res))
  }
}
