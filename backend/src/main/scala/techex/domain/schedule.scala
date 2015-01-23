package techex.domain

import org.joda.time.{Interval, Instant}

object schedule {

}


case class ScheduleInstant(instant:Instant)
case class ScheduleEvent(start:ScheduleInstant,end:ScheduleInstant)


trait Schedule{
  def instants(interval:Interval):List[ScheduleInstant]
  def eventsAt(instant:Instant):List[ScheduleEvent]
}