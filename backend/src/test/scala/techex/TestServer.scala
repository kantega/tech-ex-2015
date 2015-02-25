package techex

import argonaut.Argonaut._
import argonaut.{Json, Parse}
import com.typesafe.config.ConfigFactory
import dispatch.Defaults._
import dispatch.{host, _}
import org.http4s.server.jetty.JettyBuilder
import techex.cases.startup
import techex.domain.{Proximity, Nick, PlayerId}

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
    .mountServlet(new InitingServlet(),"/*")

  val h = host("localhost", 8080)

  val prod = host("techex.kantega.no")

  val decodeId =
    jdecode1L((value: String) => value)("id")

  def putPlayer(nick: Nick): Future[PlayerId] = {
    val putPlayerTask =
      Http(((h / "player" / nick.value) << "{'platform':{'type':'web'},'preferences':{'drink':'wine','eat':'meat'}}").PUT)

    val response =
      putPlayerTask.map(response => {
        val maybeParsedResponse: String \/ Json =
          Parse.parse(response.getResponseBody)

        val res =
          decodeId
            .decodeJson(maybeParsedResponse.getOrElse(jEmptyObject))


        val either =
          res.map(PlayerId(_)).toEither

        if (either.isLeft)
          throw new Exception("Invalid response " + response.toString)
        else
          either.right.get

      })



    response
  }

  def putObservation(playerId: PlayerId, beaconId: String, proximity: Proximity): Future[String] =
    Http(((h / "location" / playerId.value) << "{'beaconId':'" + beaconId + "','proximity':'" + proximity.toString + "'}").POST)
      .map(s => s.getStatusCode.toString)

}
