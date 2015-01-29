package techex.domain

import java.util.UUID

import org.joda.time.{Duration => Dur, _}
import techex.data.StreamEvent
import techex.domain.areas._

object schedule {

  val keyNote      = ScheduleEntry(UUID.randomUUID(), "Keynote", IntervalBounds(new DateTime(2015, 3, 18, 9, 0), Minutes.minutes(20).toStandardDuration), AreaBounds(auditorium))
  val session1     = ScheduleEntry(UUID.randomUUID(), "Session 1", IntervalBounds(new DateTime(2015, 3, 18, 9, 30), Minutes.minutes(60).toStandardDuration), AreaBounds(auditorium))
  val session2     = ScheduleEntry(UUID.randomUUID(), "Session 2", IntervalBounds(new DateTime(2015, 3, 18, 11, 0), Minutes.minutes(60).toStandardDuration), AreaBounds(auditorium))
  val session3     = ScheduleEntry(UUID.randomUUID(), "Session 3", IntervalBounds(new DateTime(2015, 3, 18, 12, 30), Minutes.minutes(60).toStandardDuration), AreaBounds(auditorium))
  val session4     = ScheduleEntry(UUID.randomUUID(), "Session 4", IntervalBounds(new DateTime(2015, 3, 18, 15, 0), Minutes.minutes(60).toStandardDuration), AreaBounds(auditorium))
  val session5     = ScheduleEntry(UUID.randomUUID(), "Session 5", IntervalBounds(new DateTime(2015, 3, 18, 16, 30), Minutes.minutes(60).toStandardDuration), AreaBounds(auditorium))
  val crowdFunding = ScheduleEntry(UUID.randomUUID(), "Crowdfunding", IntervalBounds(new DateTime(2015, 3, 18, 9, 0), Minutes.minutes(90).toStandardDuration), AreaBounds(auditorium))

  val scheduleEntries =
    List(
      keyNote,
      session1,
      session2,
      session3,
      session4,
      session5,
      crowdFunding)


  def querySchedule(pred: ScheduleEntry => Boolean) =
    scheduleEntries.filter(pred)


}


trait SpaceBounds
case class AreaBounds(area: Area) extends SpaceBounds
case object Anywhere extends SpaceBounds

trait TimeBounds {
  def abouts(instant: ReadableInstant): Boolean
}
case class IntervalBounds(start: DateTime, duration: Dur) extends TimeBounds {
  def abouts(instant: ReadableInstant) =
    new Interval(start, start.plus(duration)).contains(instant)
}
case object AnyTime extends TimeBounds {
  def abouts(instant: ReadableInstant) =
    true
}


sealed trait ScheduleMessage
case object Start extends ScheduleMessage
case object End extends ScheduleMessage

case class ScheduleEntry(id: UUID, name: String, time: IntervalBounds, space: AreaBounds)
case class ScheduleEvent(instant: DateTime, entry: ScheduleEntry, msg: ScheduleMessage) extends StreamEvent

sealed trait EntryEventInfo
case class Preminder(duration: Dur)
case class Postminder(duration: Dur)
case class Start(instant: Instant)
case class End(afterStart: Dur)


