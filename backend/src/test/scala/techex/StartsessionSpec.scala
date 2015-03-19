package techex

import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._

class StartsessionSpec  extends Specification {


  try {
    //val runningserver =
    //  server.run

    "The webserwer" should {
      "consume a list of locationupdates" in {
        val quests =
          Http((h / "sessions" / "start" / "1") POST)


        Thread.sleep(5000)

        quests().getStatusCode must_== 200
      }

    }
    //runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }


}
