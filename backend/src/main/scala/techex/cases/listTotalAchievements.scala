package techex.cases

import _root_.argonaut._
import argonaut.Argonaut._
import org.http4s.Response
import org.http4s.argonaut.ArgonautSupport._
import org.http4s.dsl._
import techex.WebHandler
import techex.data.{codecJson, Storage}
import techex.domain._

import scalaz.Scalaz._
import scalaz.{State}
import scalaz.concurrent.Task

object listTotalAchievements {

  import codecJson._

  def acheivedBy(badge: Achievement, ctx: Storage) =
    ctx.players.filter(data => data.achievements.exists(_ === badge)).map(data => data.player.nick)


  def read: State[Storage, Task[Response]] =
    State.gets(
      ctx => {
        val progress =
          quests.badges
            .map(badge => PlayerBadgeProgress(badge.id.value,badge.name,badge.desc, false))
        Ok(progress.asJson)
      }
    )

  val restApi: WebHandler = {
    case req@GET -> Root / "achievements" =>
      Storage.run(read).flatMap(i => i)
  }

}
