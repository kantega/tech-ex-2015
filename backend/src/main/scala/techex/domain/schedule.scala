package techex.domain

import java.util.UUID

import org.joda.time.Minutes._
import org.joda.time.{Duration => Dur, _}
import techex.data.StreamEvent
import techex.domain.areas._

import scalaz.Equal

object schedule {

  val keyNote      = ScheduleEntry(UUID.randomUUID(), "Keynote", IntervalBounds(new DateTime(2015, 3, 18, 9, 0), minutes(20)), AreaBounds(auditorium))
  val session1     = ScheduleEntry(UUID.randomUUID(), "Session 1", IntervalBounds(new DateTime(2015, 3, 18, 9, 30), minutes(60)), AreaBounds(auditorium))
  val session2     = ScheduleEntry(UUID.randomUUID(), "Session 2", IntervalBounds(new DateTime(2015, 3, 18, 11, 0), minutes(60)), AreaBounds(auditorium))
  val session3     = ScheduleEntry(UUID.randomUUID(), "Session 3", IntervalBounds(new DateTime(2015, 3, 18, 12, 30), minutes(60)), AreaBounds(auditorium))
  val session4     = ScheduleEntry(UUID.randomUUID(), "Session 4", IntervalBounds(new DateTime(2015, 3, 18, 15, 0), minutes(60)), AreaBounds(auditorium))
  val session5     = ScheduleEntry(UUID.randomUUID(), "Session 5", IntervalBounds(new DateTime(2015, 3, 18, 16, 30), minutes(60)), AreaBounds(auditorium))
  val crowdFunding = ScheduleEntry(UUID.randomUUID(), "Crowdfunding", IntervalBounds(new DateTime(2015, 3, 18, 9, 0), minutes(90)), AreaBounds(auditorium))

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

  def ends =
    start.plus(duration)
}

object IntervalBounds {
  def apply(readableInstant: ReadableInstant, readableDuration: ReadableDuration):IntervalBounds =
    IntervalBounds(readableInstant.toInstant.toDateTime, readableDuration.toDuration)

  def apply(readableInstant: ReadableInstant, readablePeriod: ReadablePeriod):IntervalBounds =
    IntervalBounds(readableInstant.toInstant.toDateTime, readablePeriod.toPeriod.toDurationFrom(readableInstant))
}
case object AnyTime extends TimeBounds {
  def abouts(instant: ReadableInstant) =
    true
}


sealed trait ScheduleMessage
case object Start extends ScheduleMessage
case object End extends ScheduleMessage

case class ScheduleEntry(id: UUID, name: String, time: IntervalBounds, space: AreaBounds)
object ScheduleEntry {
  implicit val scheduleEntryEqual: Equal[ScheduleEntry] =
    Equal.equalA[String].contramap((entry: ScheduleEntry) => entry.id.toString)
}
case class ScheduleEvent(instant: DateTime, entry: ScheduleEntry, msg: ScheduleMessage) extends StreamEvent

sealed trait EntryEventInfo
case class Preminder(duration: Dur)
case class Postminder(duration: Dur)
case class Start(instant: Instant)
case class End(afterStart: Dur)


