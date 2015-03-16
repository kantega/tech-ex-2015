package techex

import javax.servlet._
import javax.servlet.annotation.WebListener
import javax.servlet.http.HttpServlet

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.http4s.servlet.Http4sServlet
import org.joda.time.Instant
import techex.cases.startup
import techex.data.slack
import techex.domain.{Alert, Good}
import techex.web.WebsocketServiceServlet

import scala.collection.JavaConversions._
import scalaz.concurrent.Task

@WebListener
class Bootstrap extends ServletContextListener {

  override def contextInitialized(sce: ServletContextEvent): Unit = {
    println("Starting up app")

    val dbName =
      sce.getServletContext.getInitParameter("db_type")

    val username =
      sce.getServletContext.getInitParameter("db_username")

    val pw =
      sce.getServletContext.getInitParameter("db_password")

    val venue =
      Option(sce.getServletContext.getInitParameter("venue")).getOrElse("kantega")

    val cfgMap =
      Map("venue"->venue, "db_type" -> dbName, "db_username" -> username, "db_password" -> pw)

    val servlets =
      bootOps.servlets(cfgMap).run

    val ctx =
      sce.getServletContext


    servlets.foreach{ case (path,servlet) =>
      val restRegistration =
        ctx.addServlet(path +"api", servlet)
      restRegistration.addMapping(path)
      restRegistration.setAsyncSupported(true)
    }

  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {
    println("Shutting down app")
    slack.sendMessage("Server shutting down").run
  }
}

object bootOps {

  def servlets(cfgMap: Map[String, String]): Task[List[(String, HttpServlet)]] = {
    val cfg =
      ConfigValueFactory.fromMap(cfgMap).toConfig.withFallback(ConfigFactory.load())

    val services =
      startup.setup(cfg)
        .onFinish(maybeErr =>
        if (maybeErr.isDefined)
          slack.sendMessage("Server failed to start: " + maybeErr.get.getMessage, Alert)
        else
          slack.sendMessage("Server started at timestamp " +Instant.now().getMillis, Good)
        ).map(x =>
        List(
          "/*" -> new Http4sServlet(x._1),
          "/ws/*" -> new WebsocketServiceServlet(x._2)
        )
        )

    services

  }



}