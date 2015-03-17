package techex.cases

import argonaut.Argonaut._
import argonaut.CodecJson
import org.http4s.Header
import org.http4s.argonaut._
import org.http4s.dsl._
import org.joda.time.{Duration, DateTime}
import techex._
import techex.data._
import techex.domain.{IntervalBounds, ScheduleEntry}

import scalaz._
import techex.data.codecJson._

object listSchedule {

  implicit val timeBoundsCodec: CodecJson[IntervalBounds] =
    codec2(
      (stamp: Long, duration: Long) =>
        IntervalBounds(new DateTime(stamp), new Duration(duration)),
      (bounds: IntervalBounds) =>
        (bounds.start.getMillis,
          bounds.duration.getMillis))("startstamp", "duration")

  implicit val echeduleEntryCodec: CodecJson[ScheduleEntry] =
    casecodec6(ScheduleEntry.withStringId, ScheduleEntry.unapplyWithStringId)("id", "name", "time", "area", "started", "ended")

  def restApi: WebHandler = {
    case req@GET -> Root / "sessions" => {
      for {
        entries <- Storage.run(State.gets(sch => sch.entriesList))
        result <- Ok(entries.asJson)
      } yield result
    }
    case req@OPTIONS -> Root / any    => {
      Ok().withHeaders(Header("Access-Control-Allow-Methods", "PUT,POST, GET, OPTIONS"), Header("Access-Control-Allow-Headers", "Accept,Content-Type"), Header("Access-Control-Max-Age", "1728000"))
    }

    case req@OPTIONS -> Root / any / any2 => {
      Ok().withHeaders(Header("Access-Control-Allow-Methods", "PUT,POST, GET, OPTIONS"), Header("Access-Control-Allow-Headers", "Accept,Content-Type"), Header("Access-Control-Max-Age", "1728000"))
    }

    case req@OPTIONS -> Root / any / any2 / any3 => {
      Ok().withHeaders(Header("Access-Control-Allow-Methods", "PUT,POST, GET, OPTIONS"), Header("Access-Control-Allow-Headers", "Accept,Content-Type"), Header("Access-Control-Max-Age", "1728000"))
    }

    case req@OPTIONS -> Root / any / any2 / any3 / any4 => {
      Ok().withHeaders(Header("Access-Control-Allow-Methods", "PUT,POST, GET, OPTIONS"), Header("Access-Control-Allow-Headers", "Accept,Content-Type"), Header("Access-Control-Max-Age", "1728000"))
    }
  }
}
