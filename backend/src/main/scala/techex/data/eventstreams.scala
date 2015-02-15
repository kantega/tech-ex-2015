package techex.data

import techex.domain.{ScheduleEvent, Observation}

import scalaz.stream.async.mutable.Topic

object eventstreams {


  lazy val events: Topic[StreamEvent] =
    scalaz.stream.async.topic()


}

trait StreamEvent

case class Notification(activity:String)