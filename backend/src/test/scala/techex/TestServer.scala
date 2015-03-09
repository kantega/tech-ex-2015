package techex

import javax.servlet.http.HttpServlet

import argonaut.Argonaut._
import argonaut.{Json, Parse}
import com.typesafe.config.ConfigFactory
import dispatch.Defaults._
import dispatch.{host, _}
import org.http4s.server.jetty.JettyBuilder
import techex.cases.startup
import techex.domain._

import scala.concurrent.Future
import scalaz.\/

object TestServer {

  /*val pool =
    Executors.newFixedThreadPool(2)

  //Required for the scala Future
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutor(pool)*/


  val server = for {
    servlets <- bootOps.servlets(Map("db_type"->"mem"))
    serv <- mountServlets(servlets)(JettyBuilder.bindHttp(8080)).start
  } yield serv


  val test     = host("localhost", 8090)
  val local     = host("localhost", 8080)
  val prod     = host("techex.kantega.no").secure
  val h        = local

  val decodeId =
    jdecode1L((value: String) => value)("id")

  def putPlayer(nick: Nick): Future[PlayerId] = {
    val putPlayerTask =
      Http(((h / "players") << s"{'nick':'${nick.value}','platform':{'type':'web'},'preferences':{'drink':'wine','eat':'meat'}}").POST)

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
          throw new Exception("Invalid response " + response.getResponseBody)
        else
          either.right.get

      })



    response
  }

  def beaconAt(a: Area) = areas.beaconsAt(a).head

  def putObservation(playerId: PlayerId, beacon: BeaconId, proximity: Proximity) =
    Http(((h / "location" / playerId.value) << "{'major':'" + 1 + "','minor':" + beacon.minor + ",'proximity':'" + proximity.toString + "','activity':'enter'}").POST)

  def mountServlets(servlets: List[(String, HttpServlet)]): JettyBuilder => JettyBuilder = {
    builder => servlets.foldLeft(builder) { (builder, pair) =>
      builder.mountServlet(pair._2, pair._1, None)
    }
  }
}
