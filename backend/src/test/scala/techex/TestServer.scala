package techex

import java.util.concurrent.Executors

import dispatch.host
import org.http4s.server.jetty.JettyBuilder
import techex.cases.startup

import scala.concurrent.ExecutionContext

object TestServer {

  val pool =
    Executors.newFixedThreadPool(2)

  //Required for the scala Future
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(pool)

  val server = JettyBuilder
    .bindHttp(8080)
    .mountService(startup.setup.run, "")

  val h = host("localhost", 8080)

}
