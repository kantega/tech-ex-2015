package techex.cases

import org.http4s.{Response, Status}
import org.http4s.dsl._
import _root_.argonaut._,Argonaut._
import org.http4s.argonaut.ArgonautSupport._
import techex.WebHandler
import techex.data.{streamContext, PlayerContext}
import techex.domain._
import scalaz._,Scalaz._
import scalaz.State
import scalaz.concurrent.Task

object listTotalProgress {

  implicit val questEncode: EncodeJson[Quest] =
    EncodeJson(
      (q: Quest) =>
        ("id" := q.id.value) ->:
          ("name" := q.name) ->:
          ("desc" := q.desc) ->:
          jEmptyObject
    )

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

  implicit val progressEncodeJson: EncodeJson[QuestProgress] =
    EncodeJson(
      (progress: QuestProgress) =>
        ("quest" := progress.quest) ->:
          ("achievements" := progress.achievements) ->:
          jEmptyObject
    )


  def acheivedBy(badge: Badge, ctx: PlayerContext) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)

  def badges(quest: Quest):List[Badge] =
    quests.badges.filter(badge => badge.quest.exists(_ === quest))

  def read:State[PlayerContext,Task[Response]] =
  streamContext.read[Task[Response]](
    ctx => {
      val progress =
        quests.quests.map(quest => {
          val achievementsInQuest:List[Achievement] =
            badges(quest).map(badge => Achievement(badge, false, acheivedBy(badge, ctx)))
          QuestProgress(quest, achievementsInQuest)
        })
      Ok(progress.asJson)
    }
  )

  val restApi:WebHandler = {
    case req@GET -> Root / "quests"  =>
      streamContext.run(read).flatMap(i=>i)
  }

}
