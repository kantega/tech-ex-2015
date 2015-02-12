package techex

import javax.servlet.annotation.WebListener
import javax.servlet.http.HttpServlet
import javax.servlet._

import com.typesafe.config.ConfigFactory
import org.http4s.servlet.Http4sServlet
import techex.cases.{notifyAdmin, startup}

@WebListener
class Bootstrap extends ServletContextListener {

  override def contextInitialized(sce: ServletContextEvent): Unit = {


    val ctx =
      sce.getServletContext

    val registration =
      ctx.addServlet("example", new InitingServlet())

    registration.addMapping("/*")
    registration.setAsyncSupported(true)
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {

    notifyAdmin.sendMessage("Server shutting down").run
  }
}

class InitingServlet extends Servlet {

  var wrapped: Option[Http4sServlet] = None

  override def destroy(): Unit = {
    wrapped.foreach(_.destroy())
  }

  override def getServletConfig: ServletConfig = {
    wrapped.orNull
  }

  override def init(config: ServletConfig): Unit = {
    val dbName =
      config.getInitParameter("db")

    val username =
      config.getInitParameter("db.username")

    val pw =
      config.getInitParameter("db.password")

    val cfg =
      Map("db" -> dbName, "db.username" -> username, "db.password" -> pw)

    val service =
      startup.setup(Map()).run

    wrapped = Some(new Http4sServlet(service))

    wrapped.foreach(_.init(config))
  }

  override def getServletInfo: String = {
    "Wrapper around "+wrapped.map(_.getServletInfo).getOrElse(" unknown servlet ")
  }

  override def service(req: ServletRequest, res: ServletResponse): Unit = {
    wrapped.foreach(_.service(req,res))
  }
}
