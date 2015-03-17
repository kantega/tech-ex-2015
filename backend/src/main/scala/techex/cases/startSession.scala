package techex.cases

import org.http4s.dsl._
import org.joda.time.Instant
import techex._
import techex.data._
import techex.domain._

import scalaz.Scalaz._
import scalaz._
import scalaz.stream.async.mutable.Topic

object startSession {


  def restApi(topic: Topic[InputMessage]): WebHandler = {
    case req@POST -> Root / "sessions" / "start" / sessionId => {
      for {
        exists <- Storage.run(State.gets(sch => sch.schedule.get(ScId(sessionId)).isDefined))
        result <- if(exists) topic.publishOne(StartEntry(ScId(sessionId),Instant.now())) *> Ok() else NotFound()
      } yield result
    }
      /*To avaoid CORS crap*/
    case req@GET -> Root / "sessions" / "start" / sessionId => {
      for {
        exists <- Storage.run(State.gets(sch => sch.schedule.get(ScId(sessionId)).isDefined))
        result <- if(exists) topic.publishOne(StartEntry(ScId(sessionId),Instant.now())) *> Ok() else NotFound()
      } yield result
    }
  }
}
