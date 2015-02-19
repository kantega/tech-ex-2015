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


  def handleTracking: Process1[InputMessage, State[PlayerStore, List[FactUpdate]]] =
    process1.lift(calcActivity)


  def calcActivity: InputMessage => State[PlayerStore, List[FactUpdate]] =
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


  def nextLocation: Observation => State[PlayerStore, Option[LocationUpdate]] =
    observation => State { ctx => {

      val history =
        ctx.playerData.get(observation.playerId).map(_.movements.toList).getOrElse(nil[LocationUpdate])

      val maybeArea =
        areas.beaconPlacement.get(observation.beacon).flatMap { case (requiredProximity, area) => if (observation.proximity isSameOrCloserThan requiredProximity) Some(area) else None}

      val maybeUpdate =
        (maybeArea, history) match {
          case (None, _)         => None
          case (Some(area), Nil) => Some(LocationUpdate(observation.playerId, area, Instant.now()))
          case (Some(area), last :: rest) =>
            if (area === last.area) None
            else Some(LocationUpdate(observation.playerId, area, Instant.now()))
        }

      maybeUpdate.fold((ctx, maybeUpdate)) { update =>
        (ctx.updatePlayerData(observation.playerId, _.addMovement(update)), maybeUpdate)
      }
    }
    }


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
            .map(area => FactUpdate(UpdateMeta(locationUpdate.playerId, locationUpdate.instant), LeftArea(area)))

        val arrived =
          nextLocation
            .map(area => FactUpdate(UpdateMeta(locationUpdate.playerId, locationUpdate.instant), Entered(area)))

        val updates =
          left ::: arrived

        (ctx.addFacts(updates), updates)
      }
    }


  def meetingPoints2Activity(meetingArea: Area): LocationUpdate => State[PlayerStore, List[FactUpdate]] =
    location => State { ctx =>
      val playerData =
        ctx.playerData(location.playerId)

      val facts =
        ctx.playersPresentAt(meetingArea).filterNot(other => other === playerData)
          .flatMap(other =>
          List(
            FactUpdate(
              UpdateMeta(location.playerId, location.instant),
              MetPlayer(other.player.id, other.player.nick)),
            FactUpdate(
              UpdateMeta(other.player.id, location.instant),
              MetPlayer(playerData.player.id, playerData.player.nick))))


      val nextState =
        ctx.addFacts(facts)


      (nextState, facts)
    }


  def restApi(topic: Topic[InputMessage]): WebHandler = {
    case req@POST -> Root / "location" / playerId =>

      EntityDecoder.text(req)(body => {
        //notifyAboutUpdates.sendMessageToSlack("Request received: "+body.toString).run
        val maybeObservation =
          toJsonQuotes(body)
            .decodeValidation[ObservationData]
            .map(data => data.toObservation(PlayerId(playerId), Instant.now()))

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

}
