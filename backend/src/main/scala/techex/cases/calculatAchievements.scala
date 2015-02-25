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
          .addFacts(facts)
          .addAchievements(facts)

      (nextCtx, facts)
    }
    case fact: Fact           => State.state(nil)

  }

  def awardBadges: Fact => State[Storage, List[Fact]] = {
    case ab: EarnedAchievemnt => State { ctx =>

      val facts =
        if (ab.player.player.privateQuests.exists(_.containsAchievement(ab.achievemnt)))
          List(AwardedBadge(ab.player, Badge(ab.achievemnt),ab.instant))
        else
          List()

      val nxtctx =
        ctx.addFacts(facts)

      (nxtctx, facts)
    }
    case _: Fact              => State.state(nil)
  }
}
