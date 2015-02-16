package techex.cases

import org.http4s.dsl._
import techex._
import techex.data.{PlayerStore$, PlayerStore}
import techex.domain.PlayerId

import scalaz._

object unregisterPlayer {

  val unregisterUser: PlayerId => State[PlayerStore, Unit] =
    id =>
      State[PlayerStore, Unit](ctx =>
        (ctx.removePlayer(id), Unit))

  def restApi: WebHandler = {
    case req@DELETE -> Root / "player" / id =>
      for {
        result <- PlayerStore.run(unregisterUser(PlayerId(id)))
        r <- Ok()
      } yield r
  }
}
