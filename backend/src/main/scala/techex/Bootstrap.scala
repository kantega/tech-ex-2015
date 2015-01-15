package techex

import javax.servlet.{ServletContextEvent, ServletContextListener}
import javax.servlet.annotation.WebListener

import org.http4s.server.HttpService
import org.http4s.servlet.Http4sServlet
import techex.cases.signup
import techex.web.test

import scalaz.concurrent.Task

@WebListener
class Bootstrap extends ServletContextListener {




  override def contextInitialized(sce: ServletContextEvent): Unit = {
    val service =
      Bootstrap.setupServlet.run

    val ctx =
      sce.getServletContext

    ctx.addServlet("example", new Http4sServlet(service)).addMapping("/")
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {}
}

object Bootstrap{
  def setupServlet:Task[HttpService] = {
    for{
      _ <- signup.setup
    } yield HttpService(signup.restApi orElse test.testApi)

  }
}
