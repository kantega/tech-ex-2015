package techex.domain

import java.util.UUID

import org.joda.time.{Duration, DateTime, Instant}
import techex.data.StreamEvent
import scalaz._, Scalaz._
import scalaz.Tree

object tracking {

}


object areas {

  val allAreas = Area("everywhere")

  val foyer           = Area("foyer")
  val toiletAtSamf    = Area("toilet @ Samfundet")
  val toiletAtSeminar = Area("toilet @ Seminar")
  val stage           = Area("stage")
  val auditorium      = Area("auditorium")
  val bar             = Area("bar")
  val kantegaStand    = Area("Kantega stand")
  val technoportStand = Area("Technoport stand")
  val seminarArea     = Area("Technoport seminarområde")
  val samfundet       = Area("Samfundet")
  val kjelleren       = Area("Kjelleren")
  val technoport2015  = Area("Technoport 2015")
  val auditoriumExit  = Area("Auditorium exit")
  val coffeeStand     = Area("Coffee stand")

  val testArea1     = Area("Stand1")
  val testArea2     = Area("Stand2")
  val testArea3     = Area("Stand3")
  val kantegaCoffee     = Area("kantegaCoffee")
  val kantegaOffice = Area("KantegaOffice")

  val beaconPlacement: Map[(Beacon,Proximity), Area] =
    Map(
      (Beacon("a"),Near) -> foyer,
      (Beacon("b"),Near) -> toiletAtSamf,
      (Beacon("c"),Near) -> toiletAtSamf,
      (Beacon("d"),Near) -> stage,
      (Beacon("e"),Near) -> bar,
      (Beacon("f"),Near) -> technoportStand,
      (Beacon("g"),Near) -> kantegaStand,
      (Beacon("g"),Near) -> testArea1,
      (Beacon("g"),Near) -> testArea2,
      (Beacon("g"),Near) -> testArea3,
      (Beacon("g"),Near) -> kantegaCoffee,
      (Beacon("h"),Near) -> coffeeStand)

  val locationHierarcy: Tree[Area] =
    allAreas.node(
      technoport2015.node(
        samfundet.node(
          kjelleren.leaf,
          bar.leaf,
          toiletAtSamf.leaf,
          foyer.leaf),
        seminarArea.node(
          auditorium.node(
            stage.leaf),
          kantegaStand.leaf,
          technoportStand.leaf,
          auditoriumExit.leaf,
          toiletAtSeminar.leaf,
          coffeeStand.leaf)),
      kantegaOffice.node(
        testArea1.leaf,
        testArea2.leaf,
        testArea3.leaf
      ))

  def contains(parent: Area, other: Area): Boolean = {
    if (parent === other)
      true
    else
      areas
        .locationHierarcy
        .loc
        .find(loc => loc.getLabel === parent)
        .get
        .find(loc => loc.getLabel === other)
        .isDefined
  }
}

case class LocationId(value: String)

case class Area(id: String) {
  def contains(other: Area) =
    areas.contains(this, other)

}
object Area {
  implicit val areaEqual: Equal[Area] =
    Equal[String].contramap(_.id)
}
case class Beacon(id: String)

trait Proximity
case object Near extends Proximity
case object Far extends Proximity
case object Immediate extends Proximity

case class Observation(id: UUID, beacon: Option[Beacon], playerId: PlayerId, instant: Instant, proximity: Proximity) extends StreamEvent

case class Timed[A](timestamp: Instant, value: A)

case class LocationUpdate(id: UUID, playerId: PlayerId, area: Area, instant: Instant)

case class UpdateMeta(id: UUID, playerId: PlayerId, instant: Instant)
case class FactUpdate(info: UpdateMeta, fact: Fact)

trait Fact
case class JoinedActivity(event: ScheduleEntry) extends Fact
case class LeftActivity(event: ScheduleEntry) extends Fact
case class JoinedOnTime(event: ScheduleEntry) extends Fact
case class LeftOnTime(event: ScheduleEntry) extends Fact
case class Entered(area: Area) extends Fact
case class LeftArea(area: Area) extends Fact
trait AggregatedFact extends Fact
case class Attended(event: ScheduleEntry) extends AggregatedFact
case class CameEarly(event: ScheduleEntry, duration: Duration) extends AggregatedFact
case class CameLate(event: ScheduleEntry, duration: Duration) extends AggregatedFact
case class LeftEarly(event: ScheduleEntry, duration: Duration, cause: String) extends AggregatedFact
case class LeftFor(event: ScheduleEntry, activity: String, duration: Duration) extends AggregatedFact
case class Connected(playerId: PlayerId) extends AggregatedFact


