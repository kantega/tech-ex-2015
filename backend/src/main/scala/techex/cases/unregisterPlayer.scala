package techex.cases

import org.http4s.dsl._
import techex._
import techex.data.{PlayerContext, streamContext}
import techex.domain.PlayerId

import scalaz._

object unregisterPlayer {

  val unregisterUser: PlayerId => State[PlayerContext, Unit] =
    id =>
      State[PlayerContext, Unit](ctx =>
        (ctx.removePlayer(id), Unit))

  def restApi: WebHandler = {
    case req@DELETE -> Root / "player" / id =>
      for {
        result <- streamContext.run(unregisterUser(PlayerId(id)))
        r <- Ok()
      } yield r
  }
}
