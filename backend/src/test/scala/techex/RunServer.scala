package techex

import org.http4s.server.jetty.JettyBuilder

object RunServer extends App {


  val server = for {
    servlets <- bootOps.servlets(Map())
    serv <- TestServer.mountServlets(servlets)(JettyBuilder.bindHttp(8080)).start
  } yield serv


  server.run.awaitShutdown()



}
