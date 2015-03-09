package techex

import argonaut.Parse
import dispatch.Defaults._
import dispatch._
import org.specs2.mutable._
import techex.TestServer._

class ListSessionsSpec extends Specification {


  try {
    val runningserver =
      server.run

    "The webserwer" should {
      "yield a json structure of all the schedule entries" in {
        val sessions =
          Http((h / "sessions") GET)


        val response = sessions()

        val json =
          Parse.parse(response.getResponseBody).map(_.spaces4).fold(x=>x,y=>y)



        json ! (response.getStatusCode must_== 200)
      }

    }
    runningserver.shutdown
  } catch {
    case t: Throwable => t.printStackTrace()
  }

}
