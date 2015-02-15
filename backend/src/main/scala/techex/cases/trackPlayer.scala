package techex.cases

import java.util.UUID

import org.http4s.EntityDecoder
import org.http4s.dsl._
import org.joda.time.Instant
import techex._
import techex.cases.codecJson._
import techex.data._
import techex.domain._

import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut.ArgonautSupport._

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task
import scalaz.stream._

object trackPlayer {


  def handleTracking: Process1[StreamEvent, State[PlayerContext, List[FactUpdate]]] =
    process1.lift(event => {
      for {
        updates <- calcActivity(event)
        badges <- calcBadges(updates)
      } yield updates ++ badges
    })


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

      simpleActivities
    }

  def calcBadges: List[FactUpdate] => State[PlayerContext, List[FactUpdate]] =
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
          matcher(factUpdate)


        val nextCtx =
          ctx.updatePlayerData(playerId, PlayerData.updateProgess(next))

        (nextCtx, outUpdates ++ updates.map(b => FactUpdate(factUpdate.info, AchievedBadge(b.id.value))))
      }

    }


  def nextLocation: Observation => State[PlayerContext, Option[LocationUpdate]] =
    observation => State { ctx => {

      val history =
        ctx.playerData.get(observation.playerId).map(_.movements.toList).getOrElse(nil[LocationUpdate])

      val maybeArea =
        areas.beaconPlacement.get((observation.beacon, observation.proximity))

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
          } yield FactUpdate(UpdateMeta(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant,getNick(incomingLocation.playerId,ctx)), JoinedActivity(event))

        val leaveActivities =
          for {
            outgoingLocation <- ctx.playerData(incomingLocation.playerId).movements.tail.headOption.toList
            event <- schedule.querySchedule(_.time.abouts(incomingLocation.instant)).filter(_.space.area contains outgoingLocation.area)
          } yield FactUpdate(UpdateMeta(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant,getNick(incomingLocation.playerId,ctx)), LeftActivity(event))

        val activities = joinActivities ++ leaveActivities

        (ctx.addActivities(activities), activities)
      }
    }

  def location2VisitActivities: LocationUpdate => State[PlayerContext, List[FactUpdate]] =
    locationUpdate => State {
      ctx => {

        val lastLocations =
          ctx
            .playerData(locationUpdate.playerId)
            .movements
            .tail //The update is already prepended to history at this stage
            .headOption
            .map(_.area.withParents)
            .getOrElse(nil[Area])
            .toSet

        val nextLocations =
          locationUpdate.area
            .withParents
            .toSet



        val left =
          (lastLocations -- nextLocations)
            .map(area => FactUpdate(UpdateMeta(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant,getNick(locationUpdate.playerId,ctx)), LeftArea(area)))
            .toList

        val arrived =
          (nextLocations -- lastLocations)
            .map(area => FactUpdate(UpdateMeta(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant,getNick(locationUpdate.playerId,ctx)), Entered(area)))
            .toList.reverse

        (ctx.addActivities(left ::: arrived), left ::: arrived)
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
              .map(playerData => FactUpdate(UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant,playerData.player.nick), JoinedOnTime(event.entry)))
          }
          case End   => {
            presentPlayers
              .map(playerData => FactUpdate(UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant,playerData.player.nick), LeftOnTime(event.entry)))
          }
        }
      }

  def restApi: WebHandler = {
    case req@POST -> Root / "location" / playerId =>
      EntityDecoder.text(req)(body => {
        val maybeObservation =
          toJsonQuotes(body)
            .decodeValidation[ObservationData]
            .map(data => data.toObservation(UUID.randomUUID(), PlayerId(playerId), Instant.now()))

        maybeObservation.fold(
          failMsg =>
            BadRequest(failMsg),
          observation =>
            for {
              _ <- eventstreams.events.publishOne(observation)
              response <- Ok()
            } yield response)
      })
  }

  def getNick(playerId: PlayerId, ctx: PlayerContext): Nick =
    ctx.playerData(playerId).player.nick
}
