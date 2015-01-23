package techex

import javax.servlet.{ServletContextEvent, ServletContextListener}
import javax.servlet.annotation.WebListener

import org.http4s.server.HttpService
import org.http4s.servlet.Http4sServlet
import techex.cases.playerSignup
import techex.data.{ObservationDAO, PlayerDAO, db}
import techex.web.test

import scalaz.concurrent.Task

@WebListener
class Bootstrap extends ServletContextListener {


  override def contextInitialized(sce: ServletContextEvent): Unit = {
    val service =
      Bootstrap.setup.run

    val ctx =
      sce.getServletContext
    val registration =
      ctx.addServlet("example", new Http4sServlet(service))

    registration.addMapping("/*")
    registration.setAsyncSupported(true)
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {}
}

object Bootstrap {
  def setup: Task[HttpService] = {
    for {
      _ <- db.ds.transact(PlayerDAO.create)
      _ <- Task.delay(println("Created player table"))
      _ <- db.ds.transact(ObservationDAO.createObservationtable)
      _ <- Task.delay(println("Created observation table"))
    } yield HttpService(playerSignup.restApi orElse test.testApi)

  }
}
