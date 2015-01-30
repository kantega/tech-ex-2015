package techex.cases

import java.util.UUID

import org.joda.time.Instant
import techex.data._
import techex.domain._

import scalaz._, Scalaz._

object trackPlayer {

  import tracking._



  def calcActivity: StreamEvent => State[PlayerContext, List[Activity]] =
    event => {
      event match {
        case observation: Observation =>
          obs2Location(observation)
            .flatMap({
            case None         => State.state(nil[Activity])
            case Some(update) => for {
              a <- location2ScheduleActivity(update)
              b <- location2VisitActivities(update)
            } yield a ++ b
          })

        case scheduleEvent: ScheduleEvent =>
          scheduleEvent2Activity(scheduleEvent)

        case _                            =>
          State(ctx => (ctx, Nil))

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

  def location2ScheduleActivity(incomingLocation: LocationUpdate): State[PlayerContext, List[Activity]] = State {
    ctx => {
      val joinActivities =
        for {
          event <- schedule.querySchedule(_.time.abouts(incomingLocation.instant)).filter(_.space.area contains incomingLocation.area)
        } yield JoinedScheduledActivity(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant.toDateTime, event)

      val leaveActivities =
        for {
          outgoingLocation <- ctx.playerData(incomingLocation.playerId).movements.tail.headOption.toList
          event <- schedule.querySchedule(_.time.abouts(incomingLocation.instant)).filter(_.space.area contains outgoingLocation.area)
        } yield LeftScheduledActivity(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant.toDateTime, event)

      val activities = joinActivities ++ leaveActivities

      (ctx.addActivities(activities), activities)
    }
  }

  def location2VisitActivities: (LocationUpdate) => State[PlayerContext, List[Activity]] =
    locationUpdate => State {
      ctx => {
        val as = List(Entered(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant.toDateTime, locationUpdate.area))
        (ctx.addActivities(as), as)
      }
    }

  def scheduleEvent2Activity: ScheduleEvent => State[PlayerContext, List[Activity]] =
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
              .map(playerData => JoinedScheduledActivity(UUID.randomUUID(), playerData.player.id, event.instant, event.entry))
          }
          case End   => {
            presentPlayers
              .map(playerData => LeftScheduledActivity(UUID.randomUUID(), playerData.player.id, event.instant, event.entry))
          }
        }
      }

}
