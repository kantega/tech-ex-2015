package techex.domain

import java.util.UUID

import org.joda.time.{DateTime, Instant}
import techex.data.StreamEvent
import scalaz._, Scalaz._
import scalaz.Tree

object tracking {



}
object areas {

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

  val beaconPlacement: Map[Beacon, Area] =
    Map(
      Beacon("a") -> foyer,
      Beacon("b") -> toiletAtSamf,
      Beacon("c") -> toiletAtSamf,
      Beacon("d") -> stage,
      Beacon("e") -> bar,
      Beacon("f") -> technoportStand,
      Beacon("g") -> kantegaStand,
      Beacon("h") -> coffeeStand)

  val locationHierarcy: Tree[Area] =
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
        coffeeStand.leaf))

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
object Area{
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

sealed trait Activity{
  def id:UUID
  def playerId:PlayerId
  def instant:DateTime
}
case class JoinedScheduledActivity(id: UUID, playerId: PlayerId,instant: DateTime, event: ScheduleEntry) extends Activity
case class LeftScheduledActivity(id: UUID, playerId: PlayerId,instant: DateTime, event: ScheduleEntry) extends Activity
case class Entered(id: UUID, playerId: PlayerId,instant:DateTime, area:Area) extends Activity