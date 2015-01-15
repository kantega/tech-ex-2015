package techex.cases

import doobie.imports._
import org.http4s.Request
import techex._
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import techex.data.{connx, PlayerDAO}
import techex.domain.{PlayerId, Nick, Player}

import scalaz.concurrent.Task



object signup {
  sealed trait Signupresult
  case class SignupOk(player: Player) extends Signupresult
  case class NickTaken(nick: Nick) extends Signupresult


  val checkNickTaken: Nick => ConnectionIO[Boolean] =
    nick =>
      PlayerDAO.getPlayerByNick(nick).map(_.nonEmpty)

  val createPlayer: Nick => ConnectionIO[Player] =
    nick => {
      val player = Player(PlayerId.randomId(), nick)
      PlayerDAO.insertPlayer(player).map(any => player)
    }

  val createPlayerIfNickAvailable: Nick => Task[Signupresult] =
    nick =>
      for {
        taken <- connx.ds.transact(checkNickTaken(nick))
        rsult <- if (taken) Task.delay(NickTaken(nick)) else connx.ds.transact(createPlayer(nick)).map(SignupOk)
      } yield rsult


  val toResponse: Signupresult => Task[Response] = {
    case SignupOk(player) => Created(player.id.value)
    case NickTaken(nick) => Conflict(s"The nick ${nick.value} is taken, submit different nick")
  }

  lazy val restApi: WebHandler = {
    case req@PUT -> Root / "player" / nick =>
        for {
          result <- createPlayerIfNickAvailable(Nick(nick))
          response <- toResponse(result)
        } yield response
  }

  lazy val setup:Task[Unit] =
      connx.ds.transact(PlayerDAO.create).map(i=>println("Created database "+i))


}
