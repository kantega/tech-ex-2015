package techex.cases

import techex.data.{PlayerData, Storage}
import techex.domain._
import techex.streams

import scalaz.Scalaz._
import scalaz._

object calculatAchievements {


  def calcAchievementsAndAwardBadges: Fact => State[Storage, List[Fact]] =
    calcAchievemnts andThen streams.appendAccum(awardBadges)


  def calcAchievemnts: Fact => State[Storage, List[Fact]] = {
    case fap: FactAboutPlayer => State { ctx =>

      val matcher =
        ctx.playerData(fap.player.player.id).progress

      val (next, updates) =
        matcher(fap)

      val facts =
        updates.map(b => EarnedAchievemnt(fap.player, b, fap.instant))

      val nextCtx =
        ctx
          .updatePlayerData(fap.player.player.id, PlayerData.updateProgess(next))
          .addAchievements(facts)


      (nextCtx, facts)
    }
    case fact: Fact           => State { initCtx =>

      initCtx.players.foldLeft((initCtx, nil[Fact])) { (pair, player) =>
        val ctx =
          pair._1

        val allfacts =
          pair._2

        val matcher =
          ctx.playerData(player.player.id).progress

        val (next, updates) =
          matcher(fact)

        val facts =
          updates.map(b => EarnedAchievemnt(player, b, fact.instant))

        val nextCtx =
          ctx
            .updatePlayerData(player.player.id, PlayerData.updateProgess(next))
            .addAchievements(facts)

        (nextCtx, facts:::allfacts)
      }

    }

  }

  def awardBadges: Fact => State[Storage, List[Fact]] = {
    case ab: EarnedAchievemnt => State { ctx =>

      val facts =
        if (ab.player.player.privateQuests.exists(_.containsAchievement(ab.achievemnt)))
          List(AwardedBadge(ab.player, Badge(ab.achievemnt), ab.instant))
        else
          List()


      (ctx, facts)
    }
    case _: Fact              => State.state(nil)
  }
}
