package techex.cases

import techex.data.{PlayerData, PlayerStore}
import techex.domain.{AchievedBadge, FactUpdate}

import scalaz.Scalaz._
import scalaz._
import scalaz.stream.{process1, Process1}

object calculatAchievements {

  def calcAchievements:Process1[List[FactUpdate], State[PlayerStore, List[FactUpdate]]] =
    process1.lift(calcBadges)

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
}
