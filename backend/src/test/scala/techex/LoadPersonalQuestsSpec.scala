package techex

import techex.TestServer._
import org.specs2.mutable._
import dispatch._, Defaults._
import techex.domain.Nick
import scalaz._, Scalaz._
import _root_.argonaut._, Argonaut._


class LoadPersonalQuestsSpec extends Specification {
  try {
    val runningserver =
      server.start.run



    "The webserwer" should {
      "yield a list of quests" in {
        val quests =
          for {
            playerId <- putPlayer(Nick("balle"))
            response <- Http(h / "quests" / "player" / playerId.value).map(s => s.getResponseBody)

          } yield response



        quests() must contain("desc")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
