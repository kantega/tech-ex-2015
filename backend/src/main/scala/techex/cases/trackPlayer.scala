package techex.cases

import _root_.argonaut._
import argonaut.Argonaut._
import org.http4s.dsl._
import org.joda.time.Instant
import techex._
import techex.data._
import techex.data.codecJson._
import techex.domain._

import scalaz.Scalaz._
import scalaz._
import scalaz.stream.async.mutable.Topic

object trackPlayer {


  def calcActivity: PartialFunction[InputMessage, State[Storage, List[Fact]]] = {
    case observation: EnterObservation =>
      for {
        maybeLocationUpdate <- nextLocation(observation)
        b <- maybeLocationUpdate.map(location2VisitActivities).getOrElse(State.state[Storage, List[Fact]](nil))
        c <- maybeLocationUpdate.map(meetingPoints2Activity(areas.kantegaCoffeeUp, areas.kantegaCoffeeUp, areas.meetingRoom)).getOrElse(State.state[Storage, List[Fact]](nil))
      } yield b ::: c

    case exitObservation: ExitObservation => movedToUnknown(exitObservation)
    case endOfTenSecs: EndOfTenSecs       => stayAtArea(endOfTenSecs)
  }

  def stayAtArea: EndOfTenSecs => State[Storage, List[Fact]] =
    tick => State.gets {
      ctx => {
        tick :: ctx.players.map(player => {
          val arrived = player.lastLocation.instant
          AtArea(player, player.lastLocation.area, tick.instant, durationBetween(arrived, tick.instant))
        })
      }
    }

  def movedToUnknown: ExitObservation => State[Storage, List[Fact]] =
    exit => State {
      ctx => {

        val maybePlayer =
          ctx
            .playerData.get(exit.playerId)



        maybePlayer.fold((ctx, nil[Fact])) { (player: PlayerData) =>
          val lastLocation =
            player.lastLocation

          val nextLocation =
            LocationUpdate(player.player.id, areas.somewhere, exit.instant)

          val left =
            LeftArea(player, lastLocation.area, exit.instant)

          val arrived =
            EnteredArea(player, areas.somewhere, exit.instant)

          val updates =
            left :: arrived :: Nil

          (ctx.updatePlayerData(exit.playerId, _.addMovement(nextLocation)), updates)
        }

      }
    }


  def nextLocation: EnterObservation => State[Storage, Option[LocationUpdate]] =
    observation => State {
      ctx => {

        val maybePlayer =
          ctx.playerData.get(observation.playerId)

        val maybeArea =
          areas.beaconPlacement.get(observation.beacon).flatMap {
            case (requiredProximity, area) => if (observation.proximity isSameOrCloserThan requiredProximity) Some(area) else None
          }

        val maybeUpdate =
          maybePlayer.flatMap { player =>
            maybeArea match {
              case None       => None
              case Some(area) =>
                if (area === player.lastLocation.area) None
                else Some(LocationUpdate(observation.playerId, area, observation.instant))
            }
          }
        (ctx, maybeUpdate)
      }
    }


  def location2VisitActivities: LocationUpdate => State[Storage, List[Fact]] =
    locationUpdate => State {
      ctx => {

        val maybePlayer =
          ctx
            .playerData.get(locationUpdate.playerId)



        maybePlayer.fold((ctx, nil[Fact])) { (player: PlayerData) =>
          val lastLocation =
            player.lastLocation

          val nextLocation =
            locationUpdate

          val at =
            AtArea(player, lastLocation.area, locationUpdate.instant, durationBetween(lastLocation.instant, locationUpdate.instant))

          val left =
            LeftArea(player, lastLocation.area, locationUpdate.instant)

          val arrived =
            EnteredArea(player, nextLocation.area, locationUpdate.instant)

          val updates =
            at :: left :: arrived :: Nil

          (ctx.updatePlayerData(locationUpdate.playerId, _.addMovement(locationUpdate)), updates)
        }

      }
    }


  def meetingPoints2Activity(meetingAreas: Area*): LocationUpdate => State[Storage, List[Fact]] =
    location => State {
      ctx =>
        val maybePlayerData =
          ctx.playerData.get(location.playerId)

        maybePlayerData.fold((ctx, nil[Fact])) { playerData =>
          val facts =
            if (meetingAreas.exists(area => area === location.area))
              ctx.playersPresentAt(location.area).filterNot(other => other === playerData)
                .flatMap(other =>
                List(
                  MetPlayer(playerData, other, location.instant),
                  MetPlayer(other, playerData, location.instant)))
            else nil



          (ctx, facts)
        }

    }


  def restApi(topic: Topic[InputMessage]): WebHandler = {
    case req@POST -> Root / "location" / playerId =>

      req.decode[String] { body => {
        val maybeObservation =
          toJsonQuotes(body)
            .decodeValidation[ObservationData]
            .map(data => data.toObservation(PlayerId(playerId), Instant.now()))

        maybeObservation.fold(
          failMsg =>
            BadRequest(failMsg),
          observation =>
            for {
              _ <- eventstreams.events.publishOne(observation.fold(x => x, y => y))
              response <- Ok()
            } yield response)
      }
      }
  }

}
