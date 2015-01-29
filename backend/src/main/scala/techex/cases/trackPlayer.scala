package techex.cases

import java.util.UUID

import org.joda.time.Instant
import techex.data._
import techex.domain._

import scalaz._, Scalaz._

object trackPlayer {

  import tracking._



  def calcActivity: (PlayerContext, StreamEvent) => (PlayerContext, List[Activity]) =
    (ctx, event) => {

      event match {
        case observation: Observation     => {
null
        }
        case scheduleEvent: ScheduleEvent => {
null
        }
        case _                            => (ctx, Nil)

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

  def location2VisitActivities: (PlayerContext, LocationUpdate) => (PlayerContext, List[Activity]) =
    (ctx, locationUpdate) => {
      val as = List(Entered(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant.toDateTime, locationUpdate.area))
      (ctx.addActivities(as), as)
    }

  def scheduleEvent2Activity: (PlayerContext, ScheduleEvent) => List[Activity] =
    (ctx, event) => {
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
