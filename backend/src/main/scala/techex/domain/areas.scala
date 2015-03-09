package techex.domain

import java.util.UUID

import org.joda.time.{Duration, DateTime, Instant}
import techex.data.{ExitObservation, EnterObservation, InputMessage}
import scalaz._, Scalaz._
import scalaz.Tree


object areas {

  val somewhere = Area("somewhere")

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
  val meeting         = Area("MR")
  val mrtTuring       = Area("Turing")
  val mrtTesla        = Area("Tesla")
  val mrtEngelbart    = Area("Engelbart")
  val mrtAda          = Area("Ada")
  val mrtHopper       = Area("Hopper")
  val team            = Area("Team")
  val mrtCurie        = Area("Curie")
  val desk1           = Area("desk1")
  val desk2           = Area("desk2")
  val desk3           = Area("desk3")
  val coffeeMachines  = Area("Coffee")
  val kantegaCoffeeUp = Area("kantegaCoffeeUpstairs")
  val kantegaCoffeeDn = Area("kantegaCoffeeDownstairs")
  val kantegaFelles   = Area("felles")
  val kantegaKantine  = Area("kantegaKantine")
  val kantegaOffice   = Area("KantegaOffice")
  val meetingPoint    = Area("Meetingpoint")

  def beaconPlacementFor(r: Area, minor: Int, prox: Proximity): (BeaconId, (Proximity, Area)) =
    BeaconId(minor) ->(prox, r)

  def beaconsAt(r: Area) =
    beaconPlacement.toList.filter(_._2._2 === r).map(_._1)

  val beaconPlacement: Map[BeaconId, (Proximity, Area)] =
    Map(
      beaconPlacementFor(desk1, 1, Near),
      beaconPlacementFor(desk2, 2, Near),
      beaconPlacementFor(desk3, 3, Near),
      beaconPlacementFor(kantegaCoffeeDn, 4, Near),
      beaconPlacementFor(kantegaCoffeeUp, 5, Near),
      beaconPlacementFor(mrtTuring, 6, Near),
      beaconPlacementFor(mrtTesla, 7, Near),
      beaconPlacementFor(mrtEngelbart, 8, Near),
      beaconPlacementFor(mrtAda, 9, Near),
      beaconPlacementFor(mrtHopper, 10, Near),
      beaconPlacementFor(mrtCurie, 11, Near),
      beaconPlacementFor(kantegaKantine, 12, Far),
      beaconPlacementFor(kantegaKantine, 13, Far),
      beaconPlacementFor(kantegaKantine, 14, Far),
      beaconPlacementFor(kantegaKantine, 15, Far))


  val technoportLocationTree: Tree[Area] =
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
        meetingPoint.leaf))

  val kantegaLocationTree: Tree[Area] =
    kantegaOffice.node(
      meeting.node(
        mrtTuring.leaf,
        mrtTesla.leaf,
        mrtAda.leaf,
        mrtHopper.leaf,
        mrtCurie.leaf),
      team.node(
        desk1.leaf,
        desk2.leaf,
        desk3.leaf),
      coffeeMachines.node(
        kantegaCoffeeUp.leaf,
        kantegaCoffeeDn.leaf),
      kantegaFelles.node(
        kantegaKantine.leaf)
    )

  val locationHierarcy: Tree[Area] =
    somewhere.node(technoportLocationTree, kantegaLocationTree)

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

case class Area(name: String) {
  def contains(other: Area) =
    areas.contains(this, other)

  def withParents: List[Area] =
    areas.withParentAreas(this)
}

object Area {
  implicit val areaEqual: Equal[Area] =
    Equal[String].contramap(_.name)
}

case class BeaconId(minor: Int)

sealed trait Proximity {
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

  def unapply(p: Proximity) = Option(p.asString)

}
case object Near extends Proximity
case object Far extends Proximity
case object Immediate extends Proximity


sealed trait Direction {
  def asString = this match {
    case Exit => "exit"
    case _    => "enter"
  }
}
object Direction {
  def apply(value: String) = value.toLowerCase match {
    case "exit" => Exit
    case _      => Enter
  }


  def unapply(d: Direction) = Option(d.asString)
}
case object Enter extends Direction
case object Exit extends Direction

case class ObservationData(major: Option[Int], minor: Option[Int], proximity: Option[Proximity], activity: String) {
  def toObservation(playerId: PlayerId, instant: Instant): EnterObservation \/ ExitObservation =
    Direction(activity) match {
      case Enter => -\/(EnterObservation(BeaconId(minor.get), playerId, instant, proximity.get))
      case Exit  => \/-(ExitObservation(playerId, instant))
    }

}

case class Timed[A](timestamp: Instant, value: A)
case class LocationUpdate(playerId: PlayerId, area: Area, instant: Instant)
case class UpdateMeta(playerId: PlayerId, instant: Instant)


