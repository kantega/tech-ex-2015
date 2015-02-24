package techex.cases

import org.http4s.Response

import scalaz._, Scalaz._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut._
import techex._
import techex.data.{codecJson, Storage}
import techex.domain._

import scalaz.concurrent.Task

object listPersonalAchievements {

  import codecJson._

  def acheivedBy(badge: Achievement, ctx: Storage) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)

  val restApi: WebHandler = {
    case req@GET -> Root / "achievements" / "player" / playerId => {
      val achievemnts: Task[Task[Response]] =
        Storage.run[Task[Response]](State {
          playerContext: Storage =>
            val maybePlayerData =
              playerContext.playerData.get(PlayerId(playerId))

            maybePlayerData.fold((playerContext, NotFound())) { playerData =>
              val player =
                playerData.player

              val visibleForUser =
                player.privateQuests
                  .flatMap(_.badges)

              val progress =
                visibleForUser
                  .map(badge => PlayerBadgeProgress(badge.id.value, badge.name, badge.desc, playerData.achievements.contains(badge)))

              (playerContext, Ok(progress.asJson))
            }
        })
      achievemnts.flatMap(i => i)
    }
  }
}
