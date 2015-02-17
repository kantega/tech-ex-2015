package techex


import org.specs2.mutable._
import dispatch._, Defaults._

import TestServer._

class RegisterUserSpec extends Specification {



  val runningserver =
    server.start.run


  "The webserwer" should {
    "yield pong when pinged" in {
      val pingReq =
        Http(h / "ping" OK as.String)

      pingReq() must contain("pong")

    }


    "yield a 400 reponse when no body is set" in {

      val putPlayerTask =
        Http((h / "player" / "fatle") PUT)

      //println(body)
      putPlayerTask().getStatusCode mustEqual 400
    }

    "yield a 201 reponse when no preferences is set" in {

      val putPlayerTask =
        Http((h / "player" / "fatler") << "{'platform':{'type':'web'}}" PUT)

      //println(body)
      putPlayerTask().getStatusCode mustEqual 201
    }

    "yield a player id and 201 Created when a correct body is set" in {
      val putPlayerTask =
        Http(((h / "player" / "fatlerr") << "{'platform':{'type':'web','deviceToken':''},'preferences':{'drink':'wine','eat':'meat'}}").PUT)

      val response = putPlayerTask()
      response.getResponseBody ! ((response.getStatusCode mustEqual 201) and (response.getResponseBody.length must beGreaterThan(0)))
    }

  }
  //pool.awaitTermination(5, TimeUnit.SECONDS)
  runningserver.shutdown

}
