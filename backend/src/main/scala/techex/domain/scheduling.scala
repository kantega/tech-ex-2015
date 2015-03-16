package techex.domain

import java.util.UUID

import org.joda.time.Minutes._
import org.joda.time.{Duration => Dur, _}
import techex.data.{Command, InputMessage}
import techex.domain.areas._

import scalaz._, Scalaz._

object scheduling {

  /*
  The Entrepreneurial state of mind 1200-1330
Peace, Love and Entrepreneurship 1350-1520
Blood, SWOT and Tears 15:50-1710
Imagine 1730-1830

Crowdfunding 1730-1830

Appetite for Construction 0900-1000
The Kick Inside 1020-1200
   */
  val session0 = ScheduleEntry(ScId("1"), "The Entrepreneurial state of mind", IntervalBounds(new DateTime(2015, 3, 17, 12, 0), minutes(90)), kantegaKantine)
  val session1 = ScheduleEntry(ScId("2"), "Peace, Love and Entrepreneurship 1350-1520", IntervalBounds(new DateTime(2015, 3, 17, 13, 50), minutes(90)), kantegaKantine)
  val session2 = ScheduleEntry(ScId("3"), "Blood, SWOT and Tears", IntervalBounds(new DateTime(2015, 3, 17, 15, 50), minutes(80)), kantegaKantine)
  val session3 = ScheduleEntry(ScId("4"), "Imagine", IntervalBounds(new DateTime(2015, 3, 17, 17, 30), minutes(60)), kantegaKantine)
  val session4 = ScheduleEntry(ScId("5"), "Crowdfunding", IntervalBounds(new DateTime(2015, 3, 17, 17, 30), minutes(60)), kantegaKantine)
  val session5 = ScheduleEntry(ScId("6"), "Appetite for Construction", IntervalBounds(new DateTime(2015, 3, 18, 9, 0), minutes(60)), kantegaKantine)
  val session6 = ScheduleEntry(ScId("7"), "The Kick Inside", IntervalBounds(new DateTime(2015, 3, 18, 10, 20), minutes(100)), kantegaKantine)

  val talkAnnaKjaer = ScheduleEntry(ScId("8"),"Talk: Anna Kjær Reichert",IntervalBounds(new DateTime(2015,3,17,14,24),minutes(18)),auditorium)
  val talkSteffenWellinger = ScheduleEntry(ScId("9"),"Talk: Steffen Weillinger",IntervalBounds(new DateTime(2015,3,17,14,50),minutes(15)),auditorium)
  val talkPaulIske = ScheduleEntry(ScId("10"),"Talk: Paul Iske",IntervalBounds(new DateTime(2015,3,17,15,45),minutes(20)),auditorium)
  val talkSamQuist =  ScheduleEntry(ScId("11"),"Talk: Sam Quist",IntervalBounds(new DateTime(2015,3,17,16,6),minutes(16)),auditorium)
  val talkTobyStone =  ScheduleEntry(ScId("12"),"Talk: Toby Stone",IntervalBounds(new DateTime(2015,3,17,16,25),minutes(18)),auditorium)
  val talkMartinKupp =  ScheduleEntry(ScId("13"),"Talk: Martin Kupp",IntervalBounds(new DateTime(2015,3,18,10,29),minutes(17)),auditorium)
  val talkLuiseHelliksen =  ScheduleEntry(ScId("14"),"Talk: Luise Helliksen",IntervalBounds(new DateTime(2015,3,18,10,47),minutes(15)),auditorium)
  val talkJeffSkinner =  ScheduleEntry(ScId("15"),"Talk: Jeff Skinner",IntervalBounds(new DateTime(2015,3,18,11,3),minutes(15)),auditorium)
  val talkVilleKairamo =  ScheduleEntry(ScId("16"),"Talk: Ville Kairamo",IntervalBounds(new DateTime(2015,3,18,11,19),minutes(15)),auditorium)

  val scheduleEntries =
    List(
      session0,
      session1,
      session2,
      session3,
      session4,
      session5,
      session6)

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
case class ScheduleEntry(id: ScId, name: String, time: IntervalBounds, area: Area, started: Boolean = false) {
  def start =
    copy(started = true)

  def stop =
    copy(started = false)
}
object ScheduleEntry {
  implicit val scheduleEntryEqual: Equal[ScheduleEntry] =
    Equal.equalBy((entry: ScheduleEntry) => entry.id.value)

  implicit val scheduleEntryOrder: Order[ScheduleEntry] =
    Order.orderBy((entry: ScheduleEntry) => entry.time.start.getMillis)

  def withStringId(id: String, name: String, time: IntervalBounds, area: Area, started: Boolean) = {
    ScheduleEntry(ScId(id), name, time, area, started)
  }

  def unapplyWithStringId(entry: ScheduleEntry): Option[(String, String, IntervalBounds, Area, Boolean)] =
    ScheduleEntry.unapply(entry).map(t => t.copy(_1 = t._1.value))
}






