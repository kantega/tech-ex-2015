package techex

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain._
import areas._

class TrackUserSpec extends Specification {


  try {
    val runningserver =
      server.start.run

    "The webserwer" should {
      "consume a list of locationupdates" in {
        val quests =
          for {
            playerId <- putPlayer(Nick("balle"))
            _ <- putObservation(playerId, beaconAt(kantegaCoffeeDn), Near)
            _ <- putObservation(playerId, beaconAt(kantegaCoffeeUp), Near)
            _ <- putObservation(playerId, beaconAt(desk1), Near)
            _ <- putObservation(playerId, beaconAt(desk2), Near)
            _ <- putObservation(playerId, beaconAt(desk3), Near)
            _ <- putObservation(playerId, beaconAt(mrtEngelbart), Near)
            _ <- putObservation(playerId, beaconAt(mrtTuring), Near)
            _ <- putObservation(playerId, beaconAt(mrtAda), Near)
            _ <- putObservation(playerId, beaconAt(mrtTesla), Near)
            response <- putObservation(playerId, beaconAt(kantegaCoffeeDn), Near)
          } yield response

        Thread.sleep(5000)

        quests().getStatusCode must_== 200
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }



}
