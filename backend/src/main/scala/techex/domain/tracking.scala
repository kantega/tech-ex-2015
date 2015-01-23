package techex.domain

import java.util.UUID

import org.joda.time.Instant
import scalaz._, Scalaz._
import scalaz.Tree

object tracking {

  implicit val areaEqual:Equal[Area] =
    Equal[String].contramap(_.id)

}
object areas{

  val foyer                                   = Area("foyer")
  val toiletAtSamf                            = Area("toilet @ Samfundet")
  val toiletAtSeminar                         = Area("toilet @ Seminar")
  val stage                                   = Area("stage")
  val auditorium                              = Area("auditorium")
  val bar                                     = Area("bar")
  val kantegaStand                            = Area("Kantega stand")
  val technoportStand                         = Area("Technoport stand")
  val seminarArea                             = Area("Technoport seminarområde")
  val samfundet                               = Area("Samfundet")
  val technoport2015                          = Area("Technoport 2015")
  val auditoriumExit                          = Area("Auditorium exit")
  val beaconPlacement : Map[Beacon, Area] =
    Map(
      Beacon("a") -> foyer,
      Beacon("b") -> toiletAtSamf,
      Beacon("c") -> toiletAtSamf,
      Beacon("d") -> stage,
      Beacon("e") -> bar,
      Beacon("f") -> technoportStand,
      Beacon("g") -> kantegaStand
    )
  val locationHierarcy: Tree[Area]        =
    technoport2015.node(
      samfundet.node(
        bar.leaf,
        toiletAtSamf.leaf,
        foyer.leaf),
      seminarArea.node(
        auditorium.node(
          stage.leaf),
        kantegaStand.leaf,
        technoportStand.leaf,
        auditoriumExit.leaf,
        toiletAtSeminar.leaf))
}

case class LocationId(value: String)
case class Area(id: String)
case class Beacon(id: String)
case class Observation(id: UUID, beacon: Option[Beacon], playerId: PlayerId, instant: Instant)
case class Timed[A](timestamp: Instant, value: A)


case class Location(id: UUID, playerId: PlayerId, area: Area, instant:Instant)

sealed trait Activity
case class AttendedEvent()
