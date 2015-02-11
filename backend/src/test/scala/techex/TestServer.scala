package techex

import dispatch.host
import org.http4s.server.jetty.JettyBuilder
import techex.cases.startup

object TestServer {

  /*val pool =
    Executors.newFixedThreadPool(2)

  //Required for the scala Future
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(pool)*/

  val server = JettyBuilder
    .bindHttp(8080)
    .mountService(startup.setup(Map()).run, "")

  val h = host("localhost", 8080)

}
