package techex.cases

import techex._

import techex._
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import techex.data.{codecJson, Storage}
import techex.domain._

import scalaz._, Scalaz._

import scalaz.concurrent.Task
import org.http4s.argonaut._

object listPersonalQuests {

  import codecJson._

  def acheivedBy(badge: Achievement, ctx: Storage) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)



  def read(playerId:String): State[Storage, Task[Response]] =
    State.gets(
      playerContext => {
        val maybePlayerData =
          playerContext.playerData.get(PlayerId(playerId))

        maybePlayerData.fold(NotFound()) { playerData =>
          val player =
            playerData.player


          val progress =
            player.privateQuests.map(quest => {
              val achievementsInQuest =
                quest.badges
                  .map(badge => PlayerBadgeProgress(badge.id.value,badge.name,badge.desc,playerData.achievements.contains(badge)))
              PlayerQuestProgress(quest, achievementsInQuest)
            })
          Ok(progress.asJson)

        }
      }
    )

  val restApi: WebHandler = {
    case req@GET -> Root / "quests" / "player" / playerId =>
     Storage.run(read(playerId)).flatMap(i=>i)

  }
}
