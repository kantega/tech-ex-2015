package techex.domain

import org.joda.time.Minutes._
import org.joda.time.{Duration => Dur, _}
import techex.domain.areas._

import scalaz.Scalaz._
import scalaz._

object scheduling {

  val day1                    = new Interval(new DateTime(2015, 3, 18, 11, 0).withZone(DateTimeZone.forOffsetHours(1)), new DateTime(2015, 3, 19, 1, 0).withZone(DateTimeZone.forOffsetHours(1)))
  val day2                    = new Interval(new DateTime(2015, 3, 19, 9, 0).withZone(DateTimeZone.forOffsetHours(1)), new DateTime(2015, 3, 19, 12, 20).withZone(DateTimeZone.forOffsetHours(1)))
  val leaveEarlyFromSamfundet = day1.getEnd.withHourOfDay(22).withMinuteOfHour(15)
  val arriveearlyOnDay2       = day2.getStart.withHourOfDay(8).withMinuteOfHour(30)
  val samfundetEnds           = day2.getStart.withHourOfDay(1).withMinuteOfHour(0)

  val sessionEntrStateOfMind = ScheduleEntry(ScId("1"), "The Entrepreneurial state of mind", IntervalBounds(new DateTime(2015, 3, 18, 12, 0).withZone(DateTimeZone.forOffsetHours(1)), minutes(90)), auditorium)
  val sessionPeaceLoveAndE   = ScheduleEntry(ScId("2"), "Peace, Love and Entrepreneurship", IntervalBounds(new DateTime(2015, 3, 18, 13, 50).withZone(DateTimeZone.forOffsetHours(1)), minutes(90)), auditorium)
  val sessionBloodSwotTears  = ScheduleEntry(ScId("3"), "Blood, SWOT and Tears", IntervalBounds(new DateTime(2015, 3, 18, 15, 50).withZone(DateTimeZone.forOffsetHours(1)), minutes(80)), auditorium)
  val sessionImagine         = ScheduleEntry(ScId("4"), "Imagine", IntervalBounds(new DateTime(2015, 3, 18, 17, 30).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), auditorium)
  val sessionCrowdFund       = ScheduleEntry(ScId("5"), "Crowdfunding", IntervalBounds(new DateTime(2015, 3, 18, 18, 30).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), auditorium)
  val sessionAppetiteForC    = ScheduleEntry(ScId("6"), "Appetite for Construction", IntervalBounds(new DateTime(2015, 3, 19, 9, 0).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), auditorium)
  val sessionKickInside      = ScheduleEntry(ScId("7"), "The Kick Inside", IntervalBounds(new DateTime(2015, 3, 19, 10, 20).withZone(DateTimeZone.forOffsetHours(1)), minutes(100)), auditorium)
  val lunchDayTwo            = ScheduleEntry(ScId("17"), "Lunch Day 2", IntervalBounds(new DateTime(2015, 3, 19, 12, 0).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), auditorium)

  val scheduleEntries =
    List(
      sessionEntrStateOfMind,
      sessionPeaceLoveAndE,
      sessionBloodSwotTears,
      sessionImagine,
      sessionCrowdFund,
      sessionAppetiteForC,
      sessionKickInside,
      lunchDayTwo).sortBy(entry => entry.time.start.getMillis)

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
  def apply(readableInstant: ReadableInstant, readableDuration: ReadableDuration): IntervalBounds =
    IntervalBounds(readableInstant.toInstant.toDateTime, readableDuration.toDuration)

  def apply(readableInstant: ReadableInstant, readablePeriod: ReadablePeriod): IntervalBounds =
    IntervalBounds(readableInstant.toInstant.toDateTime, readablePeriod.toPeriod.toDurationFrom(readableInstant))
}
case object AnyTime extends TimeBounds {
  def abouts(instant: ReadableInstant) =
    true
}


case class ScId(value: String)
case class ScheduleEntry(id: ScId, name: String, time: IntervalBounds, area: Area, started: Boolean = false, ended: Boolean = false) {
  def start =
    copy(started = true)

  def stop =
    copy(ended = true)
}
object ScheduleEntry {
  implicit val scheduleEntryEqual: Equal[ScheduleEntry] =
    Equal.equalBy((entry: ScheduleEntry) => entry.id.value)

  implicit val scheduleEntryOrder: Order[ScheduleEntry] =
    Order.orderBy((entry: ScheduleEntry) => entry.time.start.getMillis)

  def withStringId(id: String, name: String, time: IntervalBounds, area: Area, started: Boolean, ended: Boolean) = {
    ScheduleEntry(ScId(id), name, time, area, started, ended)
  }

  def unapplyWithStringId(entry: ScheduleEntry): Option[(String, String, IntervalBounds, Area, Boolean, Boolean)] =
    ScheduleEntry.unapply(entry).map(t => t.copy(_1 = t._1.value))
}






