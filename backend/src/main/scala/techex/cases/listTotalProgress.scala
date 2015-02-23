package techex.cases

import org.http4s.{Response, Status}
import org.http4s.dsl._
import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import techex.WebHandler
import techex.data.{codecJson, Storage}
import techex.domain._
import scalaz._, Scalaz._
import scalaz.State
import scalaz.concurrent.Task

object listTotalProgress {

  import codecJson._

  def acheivedBy(badge: Achievement, ctx: Storage) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)


  def read: State[Storage, Task[Response]] =
    State.gets(
      ctx => {
        val players =
          ctx.players

        val progress =
          quests.quests.map(quest => {

            val questProgress =
              players.map{ player =>
                val isAssigned =
                  player.player.privateQuests.contains(quest)

                val badgeProgress =
                  quest.badges.map{ achievement =>
                    PlayerBadgeProgress(achievement.id.value,achievement.name,achievement.desc,player.achievements.contains(achievement))
                  }

                TotalAchievementProgress(player.player.id,isAssigned,badgeProgress)
              }


            TotalQuestProgress(quest,questProgress)
          })
        Ok(progress.asJson)
      }
    )

  val restApi: WebHandler = {
    case req@GET -> Root / "quests" =>
      Storage.run(read).flatMap(i => i)
  }

}
