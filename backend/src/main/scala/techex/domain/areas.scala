package techex.domain

import java.util.UUID

import org.joda.time.{Duration, DateTime, Instant}
import techex.data.StreamEvent
import scalaz._, Scalaz._
import scalaz.Tree


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
  val kantegaCoffee = Area("kantegaCoffee")
  val kantegaOffice = Area("KantegaOffice")
  val meetingPoint  = Area("Meetingpoint")

  val beaconPlacement: Map[(Beacon, Proximity), Area] =
    Map(
      (Beacon("a"), Near) -> foyer,
      (Beacon("b"), Near) -> toiletAtSamf,
      (Beacon("c"), Near) -> toiletAtSamf,
      (Beacon("d"), Near) -> stage,
      (Beacon("e"), Near) -> bar,
      (Beacon("f"), Near) -> technoportStand,
      (Beacon("gg"), Near) -> kantegaStand,
      (Beacon("g"), Near) -> testArea1,
      (Beacon("h"), Near) -> testArea2,
      (Beacon("i"), Near) -> testArea3,
      (Beacon("j"), Near) -> kantegaCoffee,
      (Beacon("k"), Near) -> coffeeStand,
      (Beacon("l"), Near) -> meetingPoint)

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
          coffeeStand.leaf,
          meetingPoint.leaf)),
      kantegaOffice.node(
        testArea1.leaf,
        testArea2.leaf,
        testArea3.leaf,
        kantegaCoffee.leaf
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

  def withParentAreas(area: Area): List[Area] = {
    def getParentMaybe(child: Option[Area]): List[Area] = {
      child match {
        case None    => nil[Area]
        case Some(a) => a :: getParentMaybe(getParentArea(a))
      }
    }
    getParentMaybe(Some(area))
  }

  def getParentArea(area: Area): Option[Area] = {
    areas
      .locationHierarcy
      .loc
      .find(loc => loc.getLabel === area)
      .get.parent.map(_.getLabel)
  }

}

case class LocationId(value: String)

case class Area(id: String) {
  def contains(other: Area) =
    areas.contains(this, other)

  def withParents: List[Area] =
    areas.withParentAreas(this)
}

object Area {
  implicit val areaEqual: Equal[Area] =
    Equal[String].contramap(_.id)
}

case class Beacon(id: String)

trait Proximity

object Proximity {
  def apply(value: String) = value.toLowerCase match {
    case "immediate" | "1" => Immediate
    case "near" | "2"      => Near
    case _                 => Far
  }

}
case object Near extends Proximity
case object Far extends Proximity
case object Immediate extends Proximity

case class ObservationData(beacon: Beacon, proximity: Proximity) {
  def toObservation(id: UUID, playerId: PlayerId, instant: Instant) =
    Observation(id, beacon, playerId, instant, proximity)
}
case class Observation(id: UUID, beacon: Beacon, playerId: PlayerId, instant: Instant, proximity: Proximity) extends StreamEvent
case class Timed[A](timestamp: Instant, value: A)
case class LocationUpdate(id: UUID, playerId: PlayerId, area: Area, instant: Instant)
case class UpdateMeta(id: UUID, playerId: PlayerId, instant: Instant, nick: Nick)
case class FactUpdate(info: UpdateMeta, fact: Fact)


