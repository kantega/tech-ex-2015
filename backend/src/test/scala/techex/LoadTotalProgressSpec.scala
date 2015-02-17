package techex

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._

class LoadTotalProgressSpec extends Specification {
  try {
    val runningserver =
      server.start.run



    "The webserwer" should {
      "yield a list of quests for one player" in {
        val questsF = Http(h / "quests")
          .map(_.getResponseBody)

        val quests =
          questsF()

        println(quests)

        quests must contain("desc")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
