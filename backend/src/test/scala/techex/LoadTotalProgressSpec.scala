package techex

import com.ning.http.client.Response
import dispatch.as
import org.http4s.dsl./
import techex.TestServer._
import org.specs2.mutable._
import dispatch._, Defaults._
import scalaz._, Scalaz._
import _root_.argonaut._, Argonaut._

import scalaz.concurrent.Task

class LoadTotalProgressSpec extends Specification {
  try {
    val runningserver =
      server.start.run



    "The webserwer" should {
      "yield a list of quests" in {
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
