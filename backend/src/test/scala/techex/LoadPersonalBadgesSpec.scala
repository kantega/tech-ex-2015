package techex

import _root_.argonaut._
import argonaut.Argonaut._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._

import scalaz._

class LoadPersonalBadgesSpec extends Specification {
  try {
    val runningserver =
      server.start.run



    "The webserwer" should {
      "yield a list of quests for a single user" in {
        val putPlayerTask =
          Http((h / "player" / "atle") << "{'drink':'wine','eat':'meat'}" PUT)

        val response =
          putPlayerTask()

        val maybeParsedResponse: String \/ Json =
          Parse.parse(response.getResponseBody)

        val decodeId =
          jdecode1L((value:String)=>value)("id")

        val quests =
          decodeId
            .decodeJson(maybeParsedResponse.getOrElse(jEmptyObject))
            .map(playerId => Http(h / "achievements" / "player" / playerId))
            .fold((str, history) => str, s => s().getResponseBody)

        println(quests)

        quests must contain("badge")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
