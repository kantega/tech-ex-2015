package techex.cases

import org.http4s.Response

import scalaz._, Scalaz._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut.ArgonautSupport._
import techex._
import techex.data.{codecJson, PlayerStore$, PlayerStore}
import techex.domain._

import scalaz.concurrent.Task

object listPersonalAchievements {

  import codecJson._

  def acheivedBy(badge: Badge, ctx: PlayerStore) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)

  val restApi: WebHandler = {
    case req@GET -> Root / "achievements" / "player" / playerId => {
      val achievemnts: Task[Task[Response]] =
        PlayerStore.run[Task[Response]](State {
          playerContext: PlayerStore =>
            val maybePlayerData =
              playerContext.playerData.get(PlayerId(playerId))

            maybePlayerData.fold((playerContext, NotFound())) { playerData =>
              val player =
                playerData.player

              val visibleForUser =
                player.privateQuests
                  .map(id => quests.questMap(Qid(id.value)))
                  .flatMap(_.badges)

              val progress =
                visibleForUser
                  .map(badge => Achievement(badge.id.value,badge.name,badge.desc, playerData.achievements.contains(badge), acheivedBy(badge, playerContext)))

              (playerContext, Ok(progress.asJson))
            }
        })
      achievemnts.flatMap(i => i)
    }
  }
}