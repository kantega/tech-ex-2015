package techex

import argonaut.Argonaut._
import argonaut.{Json, Parse}
import dispatch.Defaults._
import dispatch.{host, _}
import org.http4s.server.jetty.JettyBuilder
import techex.cases.startup
import techex.domain.{Nick, PlayerId}

import scala.concurrent.Future
import scalaz.\/

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

  val decodeId =
    jdecode1L((value: String) => value)("id")

  def putPlayer(nick: Nick): Future[PlayerId] = {
    val putPlayerTask =
      Http(((h / "player" / nick.value) << "{'drink':'wine','eat':'meat'}").PUT)

    val response =
      putPlayerTask.map(response => {
        val maybeParsedResponse: String \/ Json =
          Parse.parse(response.getResponseBody)

        val res =
          decodeId
            .decodeJson(maybeParsedResponse.getOrElse(jEmptyObject))
        res.map(PlayerId(_)).toEither.right.get



      })



    response
  }

}
