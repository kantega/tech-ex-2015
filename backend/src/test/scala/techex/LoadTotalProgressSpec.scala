package techex

import argonaut.Parse
import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain.{Near, Nick}

class LoadTotalProgressSpec extends Specification {
  try {
    val runningserver =
      server.start.run



    "The webserver" should {
      "yield a list of quests differentiation between assigned and unassigned" in {

        val quests =
          for {
            playerBalleId <- putPlayer(Nick("aaaaa"))
            playerFalleId <- putPlayer(Nick("bbbbb"))
            playerKalleId <- putPlayer(Nick("ccccc"))
            _ <- putObservation(playerBalleId, "58796:18570", Near)
            _ <- putObservation(playerFalleId, "58796:18570", Near)
            _ <- putObservation(playerBalleId, "j", Near)
            _ <- putObservation(playerFalleId, "j", Near)
            _ <- putObservation(playerKalleId, "j", Near)
            _ <- putObservation(playerBalleId, "51194:16395", Near)
            _ <- putObservation(playerBalleId, "58796:18570", Near)
            _ <- putObservation(playerBalleId, "58796:18570", Near)
            _ <- putObservation(playerBalleId, "54803:59488", Near)
            _ <- putObservation(playerBalleId, "58796:18570", Near)
            reply <- putObservation(playerBalleId, "58796:18570", Near)
          } yield reply



        quests()

        Thread.sleep(15000)

        val questR =
          Http(h / "quests")

        val response =
          questR()

        val json =
          Parse.parse(response.getResponseBody).map(_.spaces4).fold(x => x, y => y)

        json ! (response.getStatusCode must_== 200)
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
