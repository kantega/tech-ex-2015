package techex.domain

import org.joda.time.Minutes._
import org.joda.time.{Duration => Dur, _}
import techex.domain.areas._

import scalaz.Scalaz._
import scalaz._

object scheduling {

  val day1 = new Interval(new DateTime(2015, 3, 17, 11, 0).withZone(DateTimeZone.forOffsetHours(1)), new DateTime(2015, 3, 18, 1, 0).withZone(DateTimeZone.forOffsetHours(1)))
  val day2 = new Interval(new DateTime(2015, 3, 18, 9, 0).withZone(DateTimeZone.forOffsetHours(1)), new DateTime(2015, 3, 18, 12, 20).withZone(DateTimeZone.forOffsetHours(1)))

  val sessionEntrStateOfMind = ScheduleEntry(ScId("1"), "The Entrepreneurial state of mind", IntervalBounds(new DateTime(2015, 3, 17, 12, 0).withZone(DateTimeZone.forOffsetHours(1)), minutes(90)), kantegaKantine)
  val sessionPeaceLoveAndE   = ScheduleEntry(ScId("2"), "Peace, Love and Entrepreneurship", IntervalBounds(new DateTime(2015, 3, 17, 13, 50).withZone(DateTimeZone.forOffsetHours(1)), minutes(90)), kantegaKantine)
  val sessionBloodSwotTears  = ScheduleEntry(ScId("3"), "Blood, SWOT and Tears", IntervalBounds(new DateTime(2015, 3, 17, 15, 50).withZone(DateTimeZone.forOffsetHours(1)), minutes(80)), kantegaKantine)
  val sessionImagine         = ScheduleEntry(ScId("4"), "Imagine", IntervalBounds(new DateTime(2015, 3, 17, 17, 30).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), kantegaKantine)
  val sessionCrowdFund       = ScheduleEntry(ScId("5"), "Crowdfunding", IntervalBounds(new DateTime(2015, 3, 17, 17, 30).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), kantegaKantine)
  val sessionAppetiteForC    = ScheduleEntry(ScId("6"), "Appetite for Construction", IntervalBounds(new DateTime(2015, 3, 18, 9, 0).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), kantegaKantine)
  val sessionKickInside      = ScheduleEntry(ScId("7"), "The Kick Inside", IntervalBounds(new DateTime(2015, 3, 18, 10, 20).withZone(DateTimeZone.forOffsetHours(1)), minutes(100)), kantegaKantine)
  val lunchDayTwo            = ScheduleEntry(ScId("17"), "Lunch Day 2", IntervalBounds(new DateTime(2015, 3, 18, 12, 0).withZone(DateTimeZone.forOffsetHours(1)), minutes(60)), kantegaKantine)

  val talkAnnaKjaer        = ScheduleEntry(ScId("8"), "Talk: Anna Kjær Reichert", IntervalBounds(new DateTime(2015, 3, 17, 14, 24).withZone(DateTimeZone.forOffsetHours(1)), minutes(18)), auditorium)
  val talkSteffenWellinger = ScheduleEntry(ScId("9"), "Talk: Steffen Weillinger", IntervalBounds(new DateTime(2015, 3, 17, 14, 50).withZone(DateTimeZone.forOffsetHours(1)), minutes(15)), auditorium)
  val talkPaulIske         = ScheduleEntry(ScId("10"), "Talk: Paul Iske", IntervalBounds(new DateTime(2015, 3, 17, 15, 45).withZone(DateTimeZone.forOffsetHours(1)), minutes(20)), auditorium)
  val talkSamQuist         = ScheduleEntry(ScId("11"), "Talk: Sam Quist", IntervalBounds(new DateTime(2015, 3, 17, 16, 6).withZone(DateTimeZone.forOffsetHours(1)), minutes(16)), auditorium)
  val talkTobyStone        = ScheduleEntry(ScId("12"), "Talk: Toby Stone", IntervalBounds(new DateTime(2015, 3, 17, 16, 25).withZone(DateTimeZone.forOffsetHours(1)), minutes(18)), auditorium)
  val talkMartinKupp       = ScheduleEntry(ScId("13"), "Talk: Martin Kupp", IntervalBounds(new DateTime(2015, 3, 18, 10, 29).withZone(DateTimeZone.forOffsetHours(1)), minutes(17)), auditorium)
  val talkLuiseHelliksen   = ScheduleEntry(ScId("14"), "Talk: Luise Helliksen", IntervalBounds(new DateTime(2015, 3, 18, 10, 47).withZone(DateTimeZone.forOffsetHours(1)), minutes(15)), auditorium)
  val talkJeffSkinner      = ScheduleEntry(ScId("15"), "Talk: Jeff Skinner", IntervalBounds(new DateTime(2015, 3, 18, 11, 3).withZone(DateTimeZone.forOffsetHours(1)), minutes(15)), auditorium)
  val talkVilleKairamo     = ScheduleEntry(ScId("16"), "Talk: Ville Kairamo", IntervalBounds(new DateTime(2015, 3, 18, 11, 19).withZone(DateTimeZone.forOffsetHours(1)), minutes(15)), auditorium)

  val scheduleEntries =
    List(
      sessionEntrStateOfMind,
      sessionPeaceLoveAndE,
      sessionBloodSwotTears,
      sessionImagine,
      sessionCrowdFund,
      sessionAppetiteForC,
      sessionKickInside,
      talkAnnaKjaer,
      talkSteffenWellinger,
      talkPaulIske,
      talkSamQuist,
      talkTobyStone,
      talkMartinKupp,
      talkLuiseHelliksen,
      talkJeffSkinner,
      talkVilleKairamo).sortBy(entry => entry.time.start.getMillis)

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






