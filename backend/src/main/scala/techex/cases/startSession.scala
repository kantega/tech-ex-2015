package techex.cases

import org.http4s.dsl._
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
        exists <- ScheduleStore.run(State.gets(sch => sch.entries.get(ScId(sessionId)).isDefined))
        result <- if(exists) topic.publishOne(StartEntry(ScId(sessionId))) *> Ok() else NotFound()
      } yield result
    }
  }
}
