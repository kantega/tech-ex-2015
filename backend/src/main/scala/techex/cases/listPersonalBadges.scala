package techex.cases

import org.http4s.Response

import scalaz._, Scalaz._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut.ArgonautSupport._
import techex._
import techex.data.{PlayerContext, streamContext}
import techex.domain._

import scalaz.concurrent.Task

object listPersonalBadges {

  implicit val badgeEncodeJson: EncodeJson[Badge] =
    EncodeJson(
      (b: Badge) =>
        ("id" := b.id.value) ->:
          ("name" := b.name) ->:
          ("desc" := b.desc) ->:
          ("visibility" := b.visibility.toString) ->:
          jEmptyObject
    )

  implicit val achievemntEncodeJson: EncodeJson[Achievement] =
    EncodeJson(
      (a: Achievement) =>
        ("badge" := a.badge) ->:
          ("achieved" := a.achieved) ->:
          ("achievedBy" := a.achievedBy.map(_.value)) ->:
          jEmptyObject
    )

  def badges(quest: Quest) =
    quests.badges.filter(badge => badge.quest.exists(_ === quest))

  def acheivedBy(badge: Badge, ctx: PlayerContext) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)

  val restApi: WebHandler = {
    case req@GET -> Root / "achievements" / "user" / playerId => {
      val achievemnts:Task[Task[Response]] =
        streamContext.run[Task[Response]](State {
          playerContext: PlayerContext =>
            val maybePlayerData =
              playerContext.playerData.get(PlayerId(playerId))

            maybePlayerData.fold((playerContext,NotFound())) { playerData =>
              val player =
                playerData.player

              val visibleForUser =
                quests.badges
                  .filter(_.visibility === Public) ++
                  player.privateQuests
                    .map(id => quests.questMap(Qid(id.value)))
                    .flatMap(badges)

              val progress =
                visibleForUser
                  .map(badge => Achievement(badge, playerData.achievements.contains(badge), acheivedBy(badge, playerContext)))

              (playerContext, Ok(progress.asJson))
            }
        })
      achievemnts.flatMap(i=>i)
    }
  }
}
