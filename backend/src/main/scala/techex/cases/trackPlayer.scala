package techex.cases

import java.util.UUID

import org.http4s.EntityDecoder
import org.http4s.dsl._
import org.joda.time.Instant
import techex._
import techex.data._
import codecJson._
import techex.data._
import techex.domain._

import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut.ArgonautSupport._

import scalaz.Scalaz._
import scalaz._
import scalaz.stream._
import scalaz.stream.async.mutable.Topic

object trackPlayer {


  def handleTracking: Process1[StreamEvent, State[PlayerStore, List[FactUpdate]]] =
    process1.lift(event => {
      for {
        updates <- calcActivity(event)
        badges <- calcBadges(updates)
      } yield updates ++ badges
    })


  def calcActivity: StreamEvent => State[PlayerStore, List[FactUpdate]] =
    event => {
      val simpleActivities: State[PlayerStore, List[FactUpdate]] =
        event match {
          case observation: Observation =>
            for {
              maybeLocationUpdate <- nextLocation(observation)
              b <- maybeLocationUpdate.map(location2VisitActivities).getOrElse(State.state[PlayerStore, List[FactUpdate]](nil))
              c <- maybeLocationUpdate.map(meetingPoints2Activity(areas.kantegaCoffee)).getOrElse(State.state[PlayerStore, List[FactUpdate]](nil))
            } yield b ::: c

          case _ =>
            State(ctx => (ctx, Nil))
        }

      simpleActivities
    }

  def calcBadges: List[FactUpdate] => State[PlayerStore, List[FactUpdate]] =
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

        (nextCtx, outUpdates ++ updates.map(b => FactUpdate(factUpdate.info, AchievedBadge(b.name))))
      }

    }


  def nextLocation: Observation => State[PlayerStore, Option[LocationUpdate]] =
    observation => State { ctx => {

      val history =
        ctx.playerData.get(observation.playerId).map(_.movements.toList).getOrElse(nil[LocationUpdate])

      val maybeArea =
        areas.beaconPlacement.get((observation.beacon, observation.proximity))

      val maybeUpdate =
        (maybeArea, history) match {
          case (None, _)                  => None
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

  /*
  def location2ScheduleActivity: LocationUpdate => State[PlayerStore, List[FactUpdate]] =
    incomingLocation => State {
      ctx => {
        val joinActivities =
          for {
            event <- scheduling.querySchedule(_.time.abouts(incomingLocation.instant)).filter(_.area contains incomingLocation.area)
          } yield FactUpdate(UpdateMeta(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant, getNick(incomingLocation.playerId, ctx)), JoinedActivity(event))

        val leaveActivities =
          for {
            outgoingLocation <- ctx.playerData(incomingLocation.playerId).movements.tail.headOption.toList
            event <- scheduling.querySchedule(_.time.abouts(incomingLocation.instant)).filter(_.area contains outgoingLocation.area)
          } yield FactUpdate(UpdateMeta(UUID.randomUUID(), incomingLocation.playerId, incomingLocation.instant, getNick(incomingLocation.playerId, ctx)), LeftActivity(event))

        val activities = joinActivities ++ leaveActivities

        (ctx.addFacts(activities), activities)
      }
    }

  def scheduleEvent2Activity: ScheduleEvent => State[PlayerStore, List[FactUpdate]] =
    event =>
      State.gets { ctx =>
        val area =
          event.entry.area

        val presentPlayers =
          ctx.playersPresentAt(area)

        event.msg match {
          case Start =>
            presentPlayers
              .map(playerData => FactUpdate(
              UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant, playerData.player.nick),
              JoinedOnTime(event.entry)))
          case End   =>
            presentPlayers
              .map(playerData => FactUpdate(
              UpdateMeta(UUID.randomUUID(), playerData.player.id, event.instant.toInstant, playerData.player.nick),
              LeftOnTime(event.entry)))
        }
      }
*/

  def location2VisitActivities: LocationUpdate => State[PlayerStore, List[FactUpdate]] =
    locationUpdate => State {
      ctx => {

        val lastLocation =
          ctx
            .playerData(locationUpdate.playerId)
            .movements
            .tail //The update is already prepended to history at this stage
            .headOption
            .map(loc => List(loc.area))
            .getOrElse(nil[Area])

        val nextLocation =
          List(locationUpdate.area)

        val left =
          lastLocation
            .map(area => FactUpdate(UpdateMeta(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant, getNick(locationUpdate.playerId, ctx)), LeftArea(area)))

        val arrived =
          nextLocation
            .map(area => FactUpdate(UpdateMeta(UUID.randomUUID(), locationUpdate.playerId, locationUpdate.instant, getNick(locationUpdate.playerId, ctx)), Entered(area)))

        val updates =
          left ::: arrived

        (ctx.addFacts(updates), updates)
      }
    }



  def meetingPoints2Activity(meetingArea: Area): LocationUpdate => State[PlayerStore, List[FactUpdate]] =
    location => State { ctx =>
      val playerData =
        ctx.playerData(location.playerId)

      val nick =
        playerData.player.nick

      val facts =
        ctx.playersPresentAt(meetingArea).filterNot(other => other === playerData)
          .flatMap(other =>
          List(
            FactUpdate(
              UpdateMeta(UUID.randomUUID(), location.playerId, location.instant, nick),
              MetPlayer(other.player.id, other.player.nick)),
            FactUpdate(
              UpdateMeta(UUID.randomUUID(), other.player.id, location.instant, other.player.nick),
              MetPlayer(playerData.player.id, playerData.player.nick))))


      val nextState =
        ctx.addFacts(facts)


      (nextState, facts)
    }


  def restApi(topic: Topic[StreamEvent]): WebHandler = {
    case req@POST -> Root / "location" / playerId =>

      EntityDecoder.text(req)(body => {
        notifyAboutUpdates.sendMessageToSlack("Request received: "+body.toString).run
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

  def getNick(playerId: PlayerId, ctx: PlayerStore): Nick =
    ctx.playerData(playerId).player.nick
}
