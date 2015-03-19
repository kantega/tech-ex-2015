package techex

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain._
import areas._
import scalaz._, Scalaz._
import scalaz.concurrent.Task

class TrackUserSpec extends Specification {


  try {
    //val runningserver =
    //  server.run

    "The webserwer" should {
      "consume a list of locationupdates" in {
        val quests =
          for {
            playerId <- putPlayer(Nick("Fjone"))
            _ <- putObservation(playerId, beaconAt(coffeeStand), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(auditorium), Near) *> Future {Thread.sleep(1000)}
            _ <- putObservation(playerId, beaconAt(standEntrepenerurShip), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(standContext), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(coffeeStand), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(standInfo), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(coffeeStand), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(standIntention), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(standKantega), Near) *> Future {Thread.sleep(50000)}

            _ <- putObservation(playerId, beaconAt(coffeeStand), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(standProduction), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(standUse), Near) *> Future {Thread.sleep(40000)}
            _ <- putObservation(playerId, beaconAt(standUserInsight), Near) *> Future {Thread.sleep(40000)}
            response <- putObservation(playerId, beaconAt(coffeeStand), Near)
          } yield response

        Thread.sleep(500000)

        quests().getStatusCode must_== 200
      }

    }
   // runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
