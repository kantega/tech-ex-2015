package techex.cases

import org.joda.time.Instant
import techex.data.Storage
import techex.domain._

import scalaz.Scalaz._
import scalaz._

object locateOnSessionTimeBoundaries {


  def handleTimeBoundsFacts: Fact => State[Storage, List[Fact]] = {
    case a: EnteredArea => handleJoining(a)
    case l: LeftArea      => handleLeaving(l)
    case s: Started       => handleStart(s)
    case e: Ended         => handleEnd(e)
    case a@_              => State.state(nil)
  }

  def handleJoining: EnteredArea => State[Storage, List[Fact]] =
    arrival => State {
      ctx => {
        val joinActivities =
          for {
            event <- ctx.entriesList.filter(s => s.started && !s.ended && s.area === arrival.area)
          } yield JoinedActivityLate(arrival.player, event,arrival.instant)

        (ctx, joinActivities)
      }
    }

  def handleLeaving: LeftArea => State[Storage, List[Fact]] =
    leaving => State {
      ctx => {
        val leaveActivities =
          for {
            event <- ctx.entriesList.filter(s => s.started && !s.ended && (s.area === leaving.area))
          } yield LeftActivityEarly(leaving.player, event,leaving.instant)

        (ctx, leaveActivities)
      }
    }


  def handleStart: Started => State[Storage, List[Fact]] =
    started => State { ctx =>
      val joinedFacts =
        ctx.players
          .filter(_.lastLocation.area === started.entry.area)
          .map(player => JoinedOnStart(player, started.entry,started.instant))

      (ctx, joinedFacts)
    }


  def handleEnd: Ended => State[Storage, List[Fact]] =
    ended => State { ctx =>
      val endedfacts =
        ctx.players
          .filter(_.lastLocation.area === ended.entry.area)
          .map(player => LeftOnEnd(player, ended.entry,ended.instant))

      (ctx, endedfacts)
    }

}
