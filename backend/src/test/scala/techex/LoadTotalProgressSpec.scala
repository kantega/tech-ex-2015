package techex

import argonaut.Parse
import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain.areas._
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
            _ <- putObservation(playerBalleId,  beaconAt(kantegaCoffeeDn), Near)
            _ <- putObservation(playerFalleId,  beaconAt(kantegaCoffeeDn), Near)
            _ <- putObservation(playerBalleId,  beaconAt(desk1), Near)
            _ <- putObservation(playerFalleId,  beaconAt(desk1), Near)
            _ <- putObservation(playerKalleId,  beaconAt(mrtAda), Near)
            _ <- putObservation(playerBalleId,  beaconAt(desk2), Near)
            _ <- putObservation(playerBalleId,  beaconAt(mrtTuring), Near)
            _ <- putObservation(playerBalleId,  beaconAt(desk3), Near)
            _ <- putObservation(playerBalleId,  beaconAt(kantegaCoffeeUp), Near)
            _ <- putObservation(playerBalleId,  beaconAt(mrtTuring), Near)
            reply <- putObservation(playerBalleId,  beaconAt(mrtEngelbart), Near)
          } yield reply



        val putResponse =
          quests()

        Thread.sleep(5000)

        val questR =
          Http(h / "quests")

        val response =
          questR()

        val json =
          Parse.parse(response.getResponseBody).map(_.spaces4).fold(x => x, y => y)

        putResponse.getResponseBody+"-\n"+ json ! (response.getStatusCode must_== 200)
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
