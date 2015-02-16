package techex.domain

import java.util.UUID

import org.joda.time.Minutes._
import org.joda.time.{Duration => Dur, _}
import techex.data.{Command, StreamEvent}
import techex.domain.areas._

import scalaz.Equal

object scheduling {

  val keyNote      = ScheduleEntry(ScId("1"), "Keynote", IntervalBounds(new DateTime(2015, 3, 18, 9, 0), minutes(20)), auditorium)
  val session1     = ScheduleEntry(ScId("2"), "Session 1", IntervalBounds(new DateTime(2015, 3, 18, 9, 30), minutes(60)), auditorium)
  val session2     = ScheduleEntry(ScId("3"), "Session 2", IntervalBounds(new DateTime(2015, 3, 18, 11, 0), minutes(60)), auditorium)
  val session3     = ScheduleEntry(ScId("4"), "Session 3", IntervalBounds(new DateTime(2015, 3, 18, 12, 30), minutes(60)), auditorium)
  val session4     = ScheduleEntry(ScId("5"), "Session 4", IntervalBounds(new DateTime(2015, 3, 18, 15, 0), minutes(60)), auditorium)
  val session5     = ScheduleEntry(ScId("6"), "Session 5", IntervalBounds(new DateTime(2015, 3, 18, 16, 30), minutes(60)), auditorium)
  val crowdFunding = ScheduleEntry(ScId("7"), "Crowdfunding", IntervalBounds(new DateTime(2015, 3, 18, 9, 0), minutes(90)), auditorium)

  val scheduleEntries =
    List(
      keyNote,
      session1,
      session2,
      session3,
      session4,
      session5,
      crowdFunding)

  val schedule =
    scheduleEntries.map(e => (e.id,e)).toMap

  def querySchedule(pred: ScheduleEntry => Boolean) =
    scheduleEntries.filter(pred)

}



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



case class ScId(value:String)
case class ScheduleEntry(id: ScId, name: String, time: IntervalBounds, area: Area)
object ScheduleEntry {
  implicit val scheduleEntryEqual: Equal[ScheduleEntry] =
    Equal.equalA[String].contramap((entry: ScheduleEntry) => entry.id.value)
}
case class Started(instant: DateTime, entry: ScheduleEntry) extends StreamEvent
case class Ended(instant:DateTime,entry:ScheduleEntry) extends StreamEvent
case class StartEntry(entry: ScheduleEntry) extends Command
case class EndEntry(entry:ScheduleEntry) extends Command
case class AddEntry(entry:ScheduleEntry) extends Command
case class RemoveEntry(entry:ScheduleEntry) extends Command


