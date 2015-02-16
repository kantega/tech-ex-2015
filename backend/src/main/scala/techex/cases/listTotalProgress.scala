package techex.cases

import org.http4s.{Response, Status}
import org.http4s.dsl._
import _root_.argonaut._, Argonaut._
import org.http4s.argonaut.ArgonautSupport._
import techex.WebHandler
import techex.data.{codecJson, PlayerStore, PlayerStore$}
import techex.domain._
import scalaz._, Scalaz._
import scalaz.State
import scalaz.concurrent.Task

object listTotalProgress {

  import codecJson._

  def acheivedBy(badge: Badge, ctx: PlayerStore) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)


  def read: State[PlayerStore, Task[Response]] =
    PlayerStore.read[Task[Response]](
      ctx => {
        val progress =
          quests.quests.map(quest => {
            val achievementsInQuest: List[Achievement] =
              quest.badges.map(badge => Achievement(badge, false, acheivedBy(badge, ctx)))
            QuestProgress(quest, achievementsInQuest)
          })
        Ok(progress.asJson)
      }
    )

  val restApi: WebHandler = {
    case req@GET -> Root / "quests" =>
      PlayerStore.run(read).flatMap(i => i)
  }

}
