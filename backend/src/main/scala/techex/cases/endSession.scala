package techex.cases

import org.http4s.dsl._
import org.joda.time.DateTime
import techex._
import techex.data._
import techex.domain._

import scalaz.stream.async.mutable.Topic

object endSession {


  def restApi(topic: Topic[InputMessage]): WebHandler = {
    case req@POST -> Root / "sessions" / "end" / sessionId => {
      for {
        _ <- topic.publishOne(EndEntry(ScId(sessionId)))
        ok <- Ok()
      } yield ok
    }
  }

}
