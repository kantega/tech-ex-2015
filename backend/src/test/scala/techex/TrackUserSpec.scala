package techex

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain.{Near, Proximity, PlayerId, Nick}

import scala.concurrent.Future
import scalaz.\/

class TrackUserSpec extends Specification {
  try {
    val runningserver =
      server.start.run

    "The webserwer" should {
      "consume a list of locationupdates" in {
        val quests =
          for {
            playerId <- putPlayer(Nick("balle"))
            _ <- putObservation(playerId, "g", Near)
            _ <- putObservation(playerId, "g", Near)
            _ <- putObservation(playerId, "h", Near)
            _ <- putObservation(playerId, "g", Near)
            _ <- putObservation(playerId, "g", Near)
            _ <- putObservation(playerId, "i", Near)
            _ <- putObservation(playerId, "g", Near)
            _ <- putObservation(playerId, "g", Near)
            _ <- putObservation(playerId, "k", Near)
            response <- putObservation(playerId, "g", Near)
          } yield response

        Thread.sleep(5000)

        quests() must contain("200")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

  def putObservation(playerId: PlayerId, beaconId: String, proximity: Proximity): Future[String] =
      Http(((h / "location" / playerId.value) << "{'beaconId':'" + beaconId + "','proximity':'" + proximity.toString + "'}").POST)
        .map(s => s.getStatusCode.toString)

}
