package techex

import org.specs2.mutable.Specification
import techex.TestServer._
import dispatch._, Defaults._
import techex.domain.areas._
import techex.domain.{Near, Nick}

class MetOtherPlayerSpec extends Specification {
  try {
    val runningserver =
      server.run



    "The webserwer" should {
      "accept observations and emit MetPlayer events" in {
        val quests =
          for {
            playerBalleId <- putPlayer(Nick("balle"))
            playerFalleId <- putPlayer(Nick("falle"))
            playerKalleId <- putPlayer(Nick("kalle"))
            response1 <- putObservation(playerBalleId, beaconAt(mrtAda), Near)
            response2 <- putObservation(playerFalleId, beaconAt(mrtAda), Near)
            response3 <- putObservation(playerBalleId, beaconAt(kantegaCoffeeDn), Near)
            response4 <- putObservation(playerFalleId, beaconAt(kantegaCoffeeDn), Near)
            response6 <- putObservation(playerKalleId, beaconAt(kantegaCoffeeDn), Near)
          } yield response1 // + response2// + response3 + response4


        Thread.sleep(15000)
        val result =
          quests()

        println(result)

        result.getStatusCode must_== 200
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }
}
