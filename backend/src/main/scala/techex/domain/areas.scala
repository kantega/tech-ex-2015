package techex.domain

import java.util.UUID

import org.joda.time.{Duration, DateTime, Instant}
import techex.data.{Observation, InputMessage}
import scalaz._, Scalaz._
import scalaz.Tree


object areas {

  val allAreas = Region(1, "everywhere")

  val foyer           = Region(2, "foyer")
  val toiletAtSamf    = Region(3, "toilet @ Samfundet")
  val toiletAtSeminar = Region(4, "toilet @ Seminar")
  val stage           = Region(5, "stage")
  val auditorium      = Region(6, "auditorium")
  val bar             = Region(7, "bar")
  val kantegaStand    = Region(8, "Kantega stand")
  val technoportStand = Region(9, "Technoport stand")
  val seminarArea     = Region(10, "Technoport seminarområde")
  val samfundet       = Region(11, "Samfundet")
  val kjelleren       = Region(12, "Kjelleren")
  val technoport2015  = Region(13, "Technoport 2015")
  val auditoriumExit  = Region(14, "Auditorium exit")
  val coffeeStand     = Region(15, "Coffee stand")
  val storhubben      = Region(16, "Storhubben")
  val mrtTuring       = Region(17, "Turing")
  val mrtTesla        = Region(18, "Tesla")
  val mrtEngelbart    = Region(19, "Engelbart")
  val mrtAda          = Region(20, "Ada")
  val mrtHopper       = Region(21, "Hopper")
  val mrtCurie        = Region(22, "Curie")
  val testArea1       = Region(23, "Stand1")
  val testArea2       = Region(24, "Stand2")
  val testArea3       = Region(25, "Stand3")
  val kantegaCoffeeUp = Region(26, "kantegaCoffeeUpstairs")
  val kantegaCoffeeDn = Region(27, "kantegaCoffeeDownstairs")
  val kantegaKantine  = Region(28, "kantegaKantine")
  val kantegaOffice   = Region(29, "KantegaOffice")
  val meetingPoint    = Region(30, "Meetingpoint")

  def beaconFor(r: Region, minor: Int, prox: Proximity): (Beacon, (Proximity, Region)) =
    Beacon(r.id, minor) ->(prox, r)

  val beaconPlacement: Map[Beacon, (Proximity, Region)] =
    Map(
      beaconFor(testArea1, 1, Near),
      beaconFor(testArea2, 1, Near),
      beaconFor(testArea3, 1, Near),
      beaconFor(kantegaCoffeeDn, 1, Near),
      beaconFor(kantegaCoffeeUp, 1, Near),
      beaconFor(mrtTuring, 1, Near),
      beaconFor(mrtTesla, 1, Near),
      beaconFor(mrtEngelbart, 1, Near),
      beaconFor(mrtAda, 1, Near),
      beaconFor(mrtHopper, 1, Near),
      beaconFor(mrtCurie, 1, Near),
      beaconFor(kantegaKantine, 1, Near))

  lazy val regionList =
    beaconPlacement.toList.map(_._2._2)

  val locationHierarcy: Tree[Region] =
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
        mrtTuring.leaf,
        mrtTesla.leaf,
        mrtAda.leaf,
        mrtHopper.leaf,
        mrtCurie.leaf,
        testArea1.leaf,
        testArea2.leaf,
        testArea3.leaf,
        kantegaCoffeeUp.leaf,
        kantegaCoffeeDn.leaf,
        kantegaKantine.leaf
      ))

  def contains(parent: Region, other: Region): Boolean = {
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

  def withParentAreas(area: Region): List[Region] = {
    def getParentMaybe(child: Option[Region]): List[Region] = {
      child match {
        case None    => nil[Region]
        case Some(a) => a :: getParentMaybe(getParentArea(a))
      }
    }
    getParentMaybe(Some(area))
  }

  def getParentArea(area: Region): Option[Region] = {
    areas
      .locationHierarcy
      .loc
      .find(loc => loc.getLabel === area)
      .get.parent.map(_.getLabel)
  }

}

case class LocationId(value: String)

case class Region(id: Int, name: String) {
  def contains(other: Region) =
    areas.contains(this, other)

  def withParents: List[Region] =
    areas.withParentAreas(this)
}

object Region {
  implicit val areaEqual: Equal[Region] =
    Equal[String].contramap(_.name)
}

case class Beacon(major: Int, minor: Int)

trait Proximity {
  def isSameOrCloserThan(other: Proximity) =
    (this, other) match {
      case (Immediate, _)     => true
      case (Near, Far | Near) => true
      case (Far, Far)         => true
      case _                  => false
    }

  def asString = this match {
    case Immediate => "immediate"
    case Near      => "near"
    case Far       => "far"
  }

}

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
  def toObservation(playerId: PlayerId, instant: Instant) =
    Observation(beacon, playerId, instant, proximity)
}

case class Timed[A](timestamp: Instant, value: A)
case class LocationUpdate(playerId: PlayerId, area: Region, instant: Instant)
case class UpdateMeta(playerId: PlayerId, instant: Instant)


