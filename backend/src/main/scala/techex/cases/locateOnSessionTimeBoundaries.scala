package techex.cases

import org.joda.time.Instant
import techex.data.Storage
import techex.domain._

import scalaz.Scalaz._
import scalaz._

object locateOnSessionTimeBoundaries {


  def handleJoining: ArrivedAtArea => State[Storage, List[Fact]] =
    arrival => State {
      ctx => {
        val joinActivities =
          for {
            event <- ctx.entriesList.filter(s => s.time.abouts(Instant.now()) && (s.area contains arrival.area))
          } yield JoinedActivity(arrival.player, event)

        (ctx.addFacts(joinActivities), joinActivities)
      }
    }

  def handleLeaving: LeftArea => State[Storage, List[Fact]] =
    leaving => State {
      ctx => {
        val leaveActivities =
          for {
            event <- ctx.entriesList.filter(s => s.time.abouts(Instant.now()) && (s.area contains leaving.area))
          } yield LeftActivity(leaving.player, event)

        (ctx.addFacts(leaveActivities), leaveActivities)
      }
    }


  def handleStart: Started => State[Storage, List[Fact]] =
    started => State { ctx =>
      val joinedFacts =
        ctx.players
          .filter(_.movements.headOption.exists(lu => lu.area === started.entry.area))
          .map(player => JoinedOnTime(player, started.entry))

      (ctx.addFacts(joinedFacts), joinedFacts)
    }


  def handleEnd: Ended => State[Storage, List[Fact]] =
    ended => State { ctx =>
      val endedfacts =
        ctx.players
          .filter(_.movements.headOption.exists(lu => lu.area === ended.entry.area))
          .map(player => LeftOnTime(player, ended.entry))

      (ctx.addFacts(endedfacts), endedfacts)
    }

}
