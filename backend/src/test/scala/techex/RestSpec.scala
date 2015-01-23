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
    .mountService(Bootstrap.setup.run, "")
    .run

  val h = host("localhost",8080)

  "The webserwer" should {
    "yield pong when pinged" in {
      val pingReq =
        Http(h / "ping" OK as.String)

      pingReq() must contain("pong")

    }


    "yield a 400 reponse when no body is set" in {

      val putPlayerTask =
        Http((h / "player" / "atle") PUT )

      //println(body)
      putPlayerTask().getStatusCode mustEqual 400
    }

    "yield a player id and 201 Created when a correct body is set" in {
      val putPlayerTask =
        Http((h / "player" / "atle") << "{'drink':'wine','eat':'meat'}" PUT )
      val response = putPlayerTask()
      response.getResponseBody ! ((response.getStatusCode mustEqual 201) and (response.getResponseBody.length must beGreaterThan(0)))
    }

  }
  pool.awaitTermination(5,TimeUnit.SECONDS)
  server.shutdown


}
