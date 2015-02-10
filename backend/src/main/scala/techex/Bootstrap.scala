package techex

import javax.servlet.annotation.WebListener
import javax.servlet.{ServletContextEvent, ServletContextListener}

import org.http4s.servlet.Http4sServlet
import techex.cases.startup

@WebListener
class Bootstrap extends ServletContextListener {


  override def contextInitialized(sce: ServletContextEvent): Unit = {
    val service =
      startup.setup.run

    val ctx =
      sce.getServletContext
    val registration =
      ctx.addServlet("example", new Http4sServlet(service))

    registration.addMapping("/*")
    registration.setAsyncSupported(true)
  }

  override def contextDestroyed(sce: ServletContextEvent): Unit = {}
}


