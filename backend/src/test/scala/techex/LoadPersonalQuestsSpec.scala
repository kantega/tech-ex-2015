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
      server.run



    "The webserwer" should {
      "yield a list of quests" in {
        val quests =
          for {
            playerId <- putPlayer(Nick("balle"))
            r <- Http(h / "quests" / "player" / playerId.value)
          } yield r

        val response =
          quests()

        val json =
          Parse.parse(response.getResponseBody).map(_.spaces4).fold(x=>x,y=>y)

        json ! (response.getResponseBody must contain("desc"))
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
