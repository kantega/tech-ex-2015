package techex.cases

import java.util.UUID

import org.joda.time.Instant
import techex.data._
import techex.domain._

import scalaz._, Scalaz._

object trackPlayer {

  import tracking._
  import matching._
  import predicates._

  val leftActivity =
    fact({ case LeftActivity(entry) => true})

  val joinedActivity =
    fact({ case JoinedActivity(entry) => true})

  val joinedSameActivity =
    ctx({ case (FactUpdate(_, JoinedActivity(entry)), matches) if matches.exists(matched({case LeftActivity(e) => entry === e})) => true})

  val joinedActivityAtSameArea =
    ctx({ case (FactUpdate(_, JoinedActivity(entry)), matches) if matches.exists(matched({case Entered(e) => entry.space.area === e})) => true})

  val leftSameActivity =
    ctx({ case (FactUpdate(_, LeftActivity(entry)), matches) if matches.exists(matched({case JoinedActivity(e) => entry === e})) => true})

  val coffee =
    visited(areas.coffeeStand)

  val toilet =
    visited(areas.toiletAtSamf) or visited(areas.toiletAtSeminar)

  val attendedSession =
    joinedActivity ~> leftSameActivity


  val enteredArea =
    fact({ case entered: Entered => true})

  val leftArea =
    fact({ case entered: LeftArea => true})

  val arrivedEarly =
    enteredArea ~> joinedActivityAtSameArea


  def calcActivity: StreamEvent => State[PlayerContext, List[FactUpdate]] = {
    case observation: Observation =>
      obs2Location(observation)
        .flatMap({
        case None         => State.state(nil[FactUpdate])
        case Some(update) => for {
          a <- location2ScheduleActivity(update)
          b <- location2VisitActivities(update)
        } yield a ++ b
      })

    case scheduleEvent: ScheduleEvent =>
      scheduleEvent2Activity(scheduleEvent)

    case _ =>
      State(ctx => (ctx, Nil))

  }

  def calcAggregateFacts(id: PlayerId): State[PlayerContext, List[FactUpdate]] = State {
    ctx => {
      val activities = ctx.playerData(id).activities.toList
      null

    }
  }


  def nextLocation: (Observation, List[LocationUpdate]) => Option[LocationUpdate] = {
    case (observation, history) =>

      val maybeArea =
        observation.beacon.flatMap(areas.beaconPlacement.get)

      (maybeArea, history) match {
        case (None, Nil)                => None
        case (None, hist)               => None
        case (Some(area), Nil)          => Some(LocationUpdate(UUID.randomUUID(), observation.playerId, area, Instant.now()))
        case (Some(area), last :: rest) =>
          if (area === last.area) None
          else Some(LocationUpdate(UUID.randomUUID(), observation.playerId, area, Instant.now()))
      }
  }


  def obs2Location(observation: Observation): State[PlayerContext, Option[LocationUpdate]] = State {
    ctx => {

      val playerId =
        observation.playerId

      val maybeUpdated =
        for {
          player <- ctx.playerData.get(playerId)
          nextLocation <- nextLocation(observation, player.movements.toList)
        } yield (ctx.putPlayerData(playerId, player.addMovement(nextLocation)), Some(nextLocation))

      maybeUpdated.getOrElse((ctx, None))
    }
  }

  def location2ScheduleActivity(incomingLocation: LocationUpdate): State[PlayerContext, List[FactUpdate]] = State {
    ctx => {
      val joinActivities =
        for {
          event <- schedule.querySchedule(_.time.abouts(incomingLocation.instant)).filter(_.space.area contains incomingLocation.area)
        } yield FactUpdate(UpdateMeta(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant), JoinedActivity(event))

      val leaveActivities =
        for {
          outgoingLocation <- ctx.playerData(incomingLocation.playerId).movements.tail.headOption.toList
          event <- schedule.querySchedule(_.time.abouts(incomingLocation.instant)).filter(_.space.area contains outgoingLocation.area)
        } yield FactUpdate(UpdateMeta(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant), LeftActivity(event))

      val activities = joinActivities ++ leaveActivities

      (ctx.addActivities(activities), activities)
    }
  }

  def location2VisitActivities: (LocationUpdate) => State[PlayerContext, List[FactUpdate]] =
    locationUpdate => State {
      ctx => {
        val left =
          if (ctx.playerData(locationUpdate.playerId).movements.nonEmpty)
            List(FactUpdate(UpdateMeta(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant), LeftArea(ctx.playerData(locationUpdate.playerId).movements.head.area)))
          else
            List()

        val arrived =
          FactUpdate(UpdateMeta(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant), Entered(locationUpdate.area)) :: left

        (ctx.addActivities(arrived), arrived)
      }
    }

  def scheduleEvent2Activity: ScheduleEvent => State[PlayerContext, List[FactUpdate]] =
    event =>
      State.gets { ctx =>
        val area =
          event.entry.space.area

        val presentPlayers =
          ctx.playerData.toList
            .map(_._2)
            .filter(_.movements.head.area === area)

        event.msg match {
          case Start => {
            presentPlayers
              .map(playerData => FactUpdate(UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant), JoinedActivity(event.entry)))
          }
          case End   => {
            presentPlayers
              .map(playerData => FactUpdate(UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant), LeftActivity(event.entry)))
          }
        }
      }

}
