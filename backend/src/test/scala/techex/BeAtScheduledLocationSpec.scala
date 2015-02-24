package techex

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._
import techex.domain.{Near, Nick}

class BeAtScheduledLocationSpec extends Specification {
  try {
    val runningserver =
      server.start.run



    "The webserver" should {
      "yield a list of quests differentiation between assigned and unassigned" in {

        val quests =
          for {
            playerBalleId <- putPlayer(Nick("aaaaa"))
            playerFalleId <- putPlayer(Nick("bbbbb"))
            _ <- putObservation(playerBalleId, "58796:18570", Near)
            _ <- putObservation(playerFalleId, "58796:18570", Near)
            _ <- putObservation(playerBalleId, "m", Near)
            reply <- Http((h / "sessions" / "start" / "1") POST)
            _ <- putObservation(playerFalleId, "m", Near)
          } yield reply



        Thread.sleep(5000)

        val response =
          quests()


         (response.getStatusCode must_== 200)
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
