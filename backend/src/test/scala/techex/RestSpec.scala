package techex

import java.util.concurrent.{TimeUnit, Executors}

import org.http4s.server.jetty.JettyBuilder
import org.specs2.mutable._
import dispatch._, Defaults._

import scala.concurrent.ExecutionContext

class RestSpec extends Specification {

  val pool =
    Executors.newFixedThreadPool(2)

  //Required for the scala Future
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(pool)

  val server = JettyBuilder
    .bindHttp(8080)
    .mountService(Bootstrap.setupServlet.run, "")
    .run


  "The webserwer" should {
    "yield pong when pinged" in {
      val pingReq =
        Http(url("http://localhost:8080/ping") OK as.String)

      pingReq() must contain("pong")
    }


    "yield a response with the player id as input" in {
      val putPlayerReq =
        url("http://localhost:8080/player/atle")
          .setMethod("PUT")


      val putPlayerTask =
        Http(putPlayerReq)
          .map(response => response.getStatusCode + " " + response.getResponseBody)



      val body = putPlayerTask()
      //println(body)
      body must contain("201")

    }
  }
  pool.awaitTermination(5,TimeUnit.SECONDS)
  server.shutdown
}
