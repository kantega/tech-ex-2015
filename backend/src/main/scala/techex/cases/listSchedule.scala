package techex.cases

import argonaut.Argonaut._
import argonaut.CodecJson
import org.http4s.argonaut.ArgonautSupport._
import org.http4s.dsl._
import org.joda.time.{Duration, DateTime}
import techex._
import techex.data._
import techex.domain.{IntervalBounds, ScheduleEntry}

import scalaz._
import techex.data.codecJson._
object listSchedule {

  implicit val timeBoundsCodec:CodecJson[IntervalBounds] =
  codec6(
    (year:String,month:String,day:String,hour:String,minute:String,duration:String) =>
      IntervalBounds(new DateTime(year.toInt,month.toInt,day.toInt,hour.toInt,minute.toInt),new Duration(duration.toLong)),
    (bounds:IntervalBounds) =>
      (bounds.start.getYear.toString,
        bounds.start.getMonthOfYear.toString,
        bounds.start.getDayOfMonth.toString,
        bounds.start.getHourOfDay.toString,
        bounds.start.getMinuteOfHour.toString,
        bounds.duration.getMillis.toString))("year","month","day","hour","minute","duration")

  implicit val echeduleEntryCodec: CodecJson[ScheduleEntry] =
    casecodec5(ScheduleEntry.withStringId, ScheduleEntry.unapplyWithStringId)("id","name","time","area","started")

  def restApi: WebHandler = {
    case req@GET -> Root / "sessions" => {
      for {
        entries <- ScheduleStore.run(State.gets(sch => sch.entriesList))
        result <- Ok(entries.asJson)
      } yield result
    }
  }
}
