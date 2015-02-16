package techex.cases

import org.http4s.dsl._
import org.joda.time.DateTime
import techex._
import techex.data._
import techex.domain._

import scalaz.stream.async.mutable.Topic

object endSession {


  def restApi(topic: Topic[StreamEvent]): WebHandler = {
    case req@POST -> Root / "session" / "end" / sessionId => {
      val maybeEntry =
        scheduling.schedule.get(ScId(sessionId))

      maybeEntry match {
        case None        => NotFound()
        case Some(entry) =>
          for {
            b <- topic.publishOne(EndEntry(entry))
            ok <- Ok()
          } yield ok
      }

    }
  }

}
