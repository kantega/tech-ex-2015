package techex.cases

import _root_.argonaut._
import argonaut.Argonaut._
import org.http4s.Response
import org.http4s.argonaut.ArgonautSupport._
import org.http4s.dsl._
import techex.WebHandler
import techex.data.{PlayerContext, streamContext}
import techex.domain._

import scalaz.Scalaz._
import scalaz.{State}
import scalaz.concurrent.Task

object listTotalAchievements {

  implicit val questEncode: EncodeJson[Quest] =
    EncodeJson(
      (q: Quest) =>
        ("id" := q.id.value) ->:
          ("name" := q.name) ->:
          ("desc" := q.desc) ->:
          jEmptyObject
    )

  implicit val badgeEncodeJson: EncodeJson[Badge] =
    jencode4L((b: Badge) => (b.id.value, b.name, b.desc, b.visibility.toString))("id", "name", "desc", "visibility")


  implicit val achievemntEncodeJson: EncodeJson[Achievement] =
    EncodeJson(
      (a: Achievement) =>
        ("badge" := a.badge) ->:
          ("achieved" := a.achieved) ->:
          ("achievedBy" := a.achievedBy.map(_.value)) ->:
          jEmptyObject
    )


  def acheivedBy(badge: Badge, ctx: PlayerContext) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)


  def read: State[PlayerContext, Task[Response]] =
    streamContext.read[Task[Response]](
      ctx => {
        val progress =
          quests.badges
            .map(badge => Achievement(badge, false, acheivedBy(badge, ctx)))
        Ok(progress.asJson)
      }
    )

  val restApi: WebHandler = {
    case req@GET -> Root / "achievements" =>
      streamContext.run(read).flatMap(i => i)
  }

}
