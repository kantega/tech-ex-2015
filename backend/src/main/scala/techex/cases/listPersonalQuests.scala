package techex.cases

import techex._

import techex._
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import techex.data.{PlayerContext, streamContext}
import techex.domain._

import scalaz._, Scalaz._

import scalaz.concurrent.Task
import org.http4s.argonaut.ArgonautSupport._

object listPersonalQuests {

  import codecJson._

  def acheivedBy(badge: Badge, ctx: PlayerContext) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)



  def read(playerId:String): State[PlayerContext, Task[Response]] =
    streamContext.read[Task[Response]](
      playerContext => {
        val maybePlayerData =
          playerContext.playerData.get(PlayerId(playerId))

        maybePlayerData.fold(NotFound()) { playerData =>
          val player =
            playerData.player

          val personalQuests =
            player.privateQuests.map(id => quests.questMap(Qid(id.value)))

          val progress =
            personalQuests.map(quest => {
              val achievementsInQuest =
                quest.badges
                  .map(badge => Achievement(badge, playerData.achievements.contains(badge), acheivedBy(badge, playerContext)))
              QuestProgress(quest, achievementsInQuest)
            })
          Ok(progress.asJson)

        }
      }
    )

  val restApi: WebHandler = {
    case req@GET -> Root / "quests" / "player" / playerId =>
     streamContext.run(read(playerId)).flatMap(i=>i)

  }
}
