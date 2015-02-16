package techex.cases

import _root_.argonaut._
import argonaut.Argonaut._
import org.http4s.Response
import org.http4s.argonaut.ArgonautSupport._
import org.http4s.dsl._
import techex.WebHandler
import techex.data.{codecJson, PlayerStore$, PlayerStore}
import techex.domain._

import scalaz.Scalaz._
import scalaz.{State}
import scalaz.concurrent.Task

object listTotalAchievements {

  import codecJson._

  def acheivedBy(badge: Badge, ctx: PlayerStore) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)


  def read: State[PlayerStore, Task[Response]] =
    PlayerStore.read[Task[Response]](
      ctx => {
        val progress =
          quests.badges
            .map(badge => Achievement(badge, false, acheivedBy(badge, ctx)))
        Ok(progress.asJson)
      }
    )

  val restApi: WebHandler = {
    case req@GET -> Root / "achievements" =>
      PlayerStore.run(read).flatMap(i => i)
  }

}
