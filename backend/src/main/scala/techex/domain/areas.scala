package techex.domain

import org.joda.time.Instant
import techex.data.{EnterObservation, ExitObservation}

import scalaz.Scalaz._
import scalaz.{Tree, _}


object areas {

  val somewhere = Area("somewhere")

  val foyer           = Area("foyer")
  val toiletAtSamf    = Area("toilet @ Samfundet")
  val toiletAtSeminar = Area("toilet @ Seminar")
  val stage           = Area("stage")
  val auditorium      = Area("auditorium")
  val bar             = Area("bar")

  val technoportStand = Area("Technoport stand")
  val seminarArea     = Area("Technoport seminarområde")
  val samfundet       = Area("Samfundet")
  val samfStorsal     = Area("Storsalen")
  val samfKlubben     = Area("Klubben")

  val meetingRoom = Area("Meetingroom")

  val kjelleren      = Area("Kjelleren")
  val technoport2015 = Area("Technoport 2015")
  val auditoriumExit = Area("Auditorium exit")

  val standsTechEx = Area("Stands @ Technoport")

  val coffeeStand = Area("Coffee stand")

  val standIntention        = Area("Intention")
  val standContext          = Area("Context")
  val standUserInsight      = Area("UserInsight")
  val standProduction       = Area("Production")
  val standUse              = Area("Use")
  val standEntrepenerurShip = Area("Entrepeneurship")
  val standInfo             = Area("Infodesk")
  val standKantega          = Area("Kantega stand")

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
  val coffeeMachines  = Area("coffeeMachines")
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
      beaconPlacementFor(kantegaKantine, 15, Far),

      beaconPlacementFor(auditorium, 101, Far),
      beaconPlacementFor(auditorium, 102, Far),
      beaconPlacementFor(auditorium, 103, Far),
      beaconPlacementFor(auditorium, 104, Far),
      beaconPlacementFor(stage, 105, Near),

      beaconPlacementFor(coffeeStand, 106, Near),
      beaconPlacementFor(coffeeStand, 107, Near),
      beaconPlacementFor(coffeeStand, 108, Near),

      beaconPlacementFor(standIntention, 109, Near),
      beaconPlacementFor(standContext, 110, Near),
      beaconPlacementFor(standUserInsight, 111, Near),
      beaconPlacementFor(standProduction, 112, Near),
      beaconPlacementFor(standUse, 113, Near),
      beaconPlacementFor(standEntrepenerurShip, 114, Near),
      beaconPlacementFor(standKantega, 115, Near),

      beaconPlacementFor(samfStorsal, 116, Far),
      beaconPlacementFor(samfStorsal, 117, Far),
      beaconPlacementFor(samfStorsal, 118, Far),
      beaconPlacementFor(samfStorsal, 119, Far),

      beaconPlacementFor(samfKlubben, 120, Far),
      beaconPlacementFor(samfKlubben, 121, Far),

      beaconPlacementFor(meetingRoom, 122, Far),
      beaconPlacementFor(standInfo, 123, Far)
    )


  val technoportLocationTree: Tree[Area] =
    technoport2015.node(
      samfundet.node(
        samfStorsal.leaf,
        samfKlubben.leaf),
      seminarArea.node(
        auditorium.leaf,
        stage.leaf),
      standsTechEx.node(
        standKantega.leaf,
        standContext.leaf,
        standEntrepenerurShip.leaf,
        standIntention.leaf,
        standProduction.leaf,
        standUse.leaf,
        standInfo.leaf,
        coffeeStand.leaf),
      meeting.node(
        meetingRoom.leaf
      ))

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


