package techex.cases

import techex.data.PlayerStore
import techex.domain.{FactUpdate, ScheduleEvent}
import scalaz.Scalaz._
import scalaz._
import scalaz.stream.{process1, Process1}

object locateOnSessionTimeBoundaries {

  /*
def location2ScheduleActivity: FactUpdate => State[PlayerStore, List[FactUpdate]] =
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
}
