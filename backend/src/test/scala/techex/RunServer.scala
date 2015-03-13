package techex

import org.http4s.server.jetty.JettyBuilder

object RunServer extends App {


  val server = for {
    servlets <- bootOps.servlets(Map("db_type"->"mem","venue"->"kantega"))
    serv <- TestServer.mountServlets(servlets)(JettyBuilder.bindHttp(8080,"172.16.0.26")).start
  } yield serv


  server.run.awaitShutdown()



}
