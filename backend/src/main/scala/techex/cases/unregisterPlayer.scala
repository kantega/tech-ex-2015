package techex.cases

import org.http4s.dsl._
import techex._
import techex.data. Storage
import techex.domain.PlayerId

import scalaz._

object unregisterPlayer {

  val unregisterUser: PlayerId => State[Storage, Unit] =
    id =>
      State[Storage, Unit](ctx =>
        (ctx.removePlayer(id), Unit))

  def restApi: WebHandler = {
    case req@DELETE -> Root / "player" / id =>
      for {
        result <- Storage.run(unregisterUser(PlayerId(id)))
        r <- Ok()
      } yield r
  }
}
