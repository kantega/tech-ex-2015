package techex

import dispatch._
import org.specs2.mutable._
import techex.TestServer._

class LoadAllAchievementsSpec extends Specification {
  try {
    val runningserver =
      server.run

    "The webserwer" should {
      "yield a list of achievemnts" in {
        val questsF = Http(h / "quests")
          .map(_.getResponseBody)

        val quests =
          questsF()

        println(quests)

        quests must contain("quest")
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
