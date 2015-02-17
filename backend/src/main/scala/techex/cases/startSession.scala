package techex.cases

import org.http4s.dsl._
import org.joda.time.{DateTime, Instant}
import techex._
import techex.data._
import techex.domain._

import scalaz.stream.async.mutable.Topic

object startSession {


  def restApi(topic: Topic[StreamEvent]): WebHandler = {
    case req@POST -> Root / "session" / "start" / sessionId => {
      for {
        _ <- topic.publishOne(StartEntry(ScId(sessionId)))
        ok <- Ok()
      } yield ok
    }
  }
}
