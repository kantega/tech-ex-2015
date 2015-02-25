package techex.domain

import java.util.UUID

import org.joda.time.{Duration, DateTime, Instant}
import techex.data.{Observation, InputMessage}
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
  val storhubben      = Area("Storhubben")
  val mrtTuring       = Area("Turing")
  val mrtTesla        = Area("Tesla")
  val mrtEngelbart    = Area("Engelbart")
  val mrtAda          = Area("Ada")
  val mrtHopper       = Area("Hopper")
  val mrtCurie       = Area("Curie")
  val testArea1     = Area("Stand1")
  val testArea2     = Area("Stand2")
  val testArea3     = Area("Stand3")
  val kantegaCoffee = Area("kantegaCoffee")
  val kantegaOffice = Area("KantegaOffice")
  val meetingPoint  = Area("Meetingpoint")

  val beaconPlacement: Map[Beacon, (Proximity, Area)] =
    Map(
      Beacon("a") ->(Near, foyer),
      Beacon("b") ->(Near, toiletAtSamf),
      Beacon("c") ->(Near, toiletAtSamf),
      Beacon("d") ->(Near, stage),
      Beacon("e") ->(Near, bar),
      Beacon("f") ->(Near, technoportStand),
      Beacon("gg") ->(Near, kantegaStand),
      Beacon("58796:18570") ->(Near, testArea1),
      Beacon("51194:16395") ->(Near, testArea2),
      Beacon("54803:59488") ->(Near, testArea3),
      Beacon("64915:4698") ->(Near, kantegaCoffee),
      Beacon("k") ->(Near, coffeeStand),
      Beacon("l") ->(Near, meetingPoint),
      Beacon("40647:50232") ->(Near, mrtTuring),
      Beacon("11910:28667") ->(Near, mrtTesla),
      Beacon("33505:43782") ->(Near, mrtEngelbart),
      Beacon("23114:24160") ->(Near, mrtAda),
      Beacon("27012:1190") ->(Near, mrtHopper),
      Beacon("31470: 23971") ->(Near, mrtCurie),
      Beacon("m") ->(Far, auditorium))

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
case class LocationUpdate(playerId: PlayerId, area: Area, instant: Instant)
case class UpdateMeta(playerId: PlayerId, instant: Instant)


