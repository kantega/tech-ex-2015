package techex

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain.{Near, Proximity, PlayerId, Nick}

import scala.concurrent.Future
import scalaz.\/

class TrackUserSpec extends Specification {
  /*
  (Beacon("58796:18570"), Near) -> testArea1,
      (Beacon("51194:16395"), Near) -> testArea2,
      (Beacon("54803:59488"), Near) -> testArea3,
  */


  try {
    val runningserver =
      server.start.run

    "The webserwer" should {
      "consume a list of locationupdates" in {
        val quests =
          for {
            playerId <- putPlayer(Nick("balle"))
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "51194:16395", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "54803:59488", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "58796:18570", Near)
            _ <- putObservation(playerId, "k", Near)
            response <- putObservation(playerId, "58796:18570", Near)
          } yield response

        Thread.sleep(5000)

        quests() must contain("200")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }



}
