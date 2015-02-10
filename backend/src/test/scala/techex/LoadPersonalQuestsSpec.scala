package techex

import techex.TestServer._
import org.specs2.mutable._
import dispatch._, Defaults._
import scalaz._, Scalaz._
import _root_.argonaut._, Argonaut._


class LoadPersonalQuestsSpec extends Specification {
  try {
    val runningserver =
      server.start.run



    "The webserwer" should {
      "yield a list of quests" in {
        val putPlayerTask =
          Http((h / "player" / "atle") << "{'drink':'wine','eat':'meat'}" PUT)

        val response =
          putPlayerTask()

        val maybeParsedResponse: String \/ Json =
          Parse.parse(response.getResponseBody)

        val decodeId =
          jdecode1L((value: String) => value)("id")

        val quests =
          decodeId
            .decodeJson(maybeParsedResponse.getOrElse(jEmptyObject))
            .map(playerId => Http(h / "quests" / "user" / playerId))
            .fold((str, history) => str, s => s().getResponseBody)

        println(quests)

        quests must contain("quest")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
