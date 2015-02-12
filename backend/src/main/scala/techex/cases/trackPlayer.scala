package techex.cases

import java.util.UUID

import org.http4s.dsl._
import org.joda.time.Instant
import techex.WebHandler
import techex.data._
import techex.domain._

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task

object trackPlayer {

  def calcActivity: StreamEvent => State[PlayerContext, List[FactUpdate]] =
    event => {
      val simpleActivities: State[PlayerContext, List[FactUpdate]] =
        event match {
          case observation: Observation =>
            for {
              maybeLocationUpdate <- nextLocation(observation)
              a <- maybeLocationUpdate.fold(State.state[PlayerContext, List[FactUpdate]](nil))(location2ScheduleActivity)
              b <- maybeLocationUpdate.fold(State.state[PlayerContext, List[FactUpdate]](nil))(location2VisitActivities)
            } yield a ++ b

          case scheduleEvent: ScheduleEvent =>
            scheduleEvent2Activity(scheduleEvent)

          case _ =>
            State(ctx => (ctx, Nil))
        }

      val aggregatesAndSimple: State[PlayerContext, List[FactUpdate]] =
        for {
          updates <- simpleActivities
          aggregates <- aggregateFacts(updates)
        } yield updates ++ aggregates

      aggregatesAndSimple
    }

  def aggregateFacts: List[FactUpdate] => State[PlayerContext, List[FactUpdate]] =
    inFactUpdates => State { inctx =>

      inFactUpdates.foldLeft((inctx, nil[FactUpdate])) { (pair, factUpdate) =>
        val ctx =
          pair._1

        val outUpdates =
          pair._2

        val playerId =
          factUpdate.info.playerId

        val matcher =
          ctx.playerData(factUpdate.info.playerId).progress

        val (next, updates) =
          matcher(Token(factUpdate, Nil))


        val nextCtx =
          ctx.updatePlayerData(playerId, PlayerData.updateProgess(next))

        (nextCtx, updates ++ outUpdates)
      }

    }


  def nextLocation: Observation => State[PlayerContext, Option[LocationUpdate]] =
    observation => State { ctx => {

      val history =
        ctx.playerData.get(observation.playerId).map(_.movements.toList).getOrElse(nil[LocationUpdate])

      val maybeArea =
        observation.beacon.flatMap(areas.beaconPlacement.get)

      val maybeUpdate =
        (maybeArea, history) match {
          case (None, Nil)                => None
          case (None, hist)               => None
          case (Some(area), Nil)          => Some(LocationUpdate(UUID.randomUUID(), observation.playerId, area, Instant.now()))
          case (Some(area), last :: rest) =>
            if (area === last.area) None
            else Some(LocationUpdate(UUID.randomUUID(), observation.playerId, area, Instant.now()))
        }

      maybeUpdate.fold((ctx, maybeUpdate)) { update =>
        (ctx.updatePlayerData(observation.playerId, _.addMovement(update)), maybeUpdate)
      }
    }
    }

  def location2ScheduleActivity: LocationUpdate => State[PlayerContext, List[FactUpdate]] =
    incomingLocation => State {
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

  def location2VisitActivities: LocationUpdate => State[PlayerContext, List[FactUpdate]] =
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
              .map(playerData => FactUpdate(UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant), JoinedOnTime(event.entry)))
          }
          case End   => {
            presentPlayers
              .map(playerData => FactUpdate(UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant), LeftOnTime(event.entry)))
          }
        }
      }

  def restApi: WebHandler = {
    case req@POST -> Root / "location" / playerId =>
      Task({
        println(s"Locationupdate received from $playerId")
      }).flatMap(x => Ok("{\"result\":\"ok\"}"))
  }
}
