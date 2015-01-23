package techex.cases

import doobie.imports._
import techex._
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import techex.data.{db, PlayerDAO}
import techex.domain._

import scalaz._, Scalaz._

import scalaz.concurrent.Task


object playerSignup {

  implicit def playerCodecJson: CodecJson[Player] =
    CodecJson(
      (p: Player) =>
        ("nick" := p.nick.value) ->:
          ("id" := p.id.value) ->:
          ("preferences" := p.preference) ->:
          ("quests" := p.privateQuests.map(_.value)) ->:
          jEmptyObject,
      c => for {
        id <- (c --\ "id").as[String]
        nick <- (c --\ "nick").as[String]
        preference <- (c --\ "preferences").as[Option[PlayerPreference]]
        privateQuests <- (c --\ "quests").as[List[String]]
      } yield
        Player(
          PlayerId(id),
          Nick(nick),
          preference.getOrElse(PlayerPreference(Coke(), Salad())),
          privateQuests.map(QuestId))
    )

  implicit def playerPreferenceCode: CodecJson[PlayerPreference] =
    casecodec2(
      (drinkS: String, eatS: String) => PlayerPreference(Drink(drinkS), Eat(eatS)),
      (preference: PlayerPreference) => Some(preference.drink.asString, preference.eat.asString)
    )("drink", "eat")

  sealed trait Signupresult
  case class SignupOk(player: Player) extends Signupresult
  case class NickTaken(nick: Nick) extends Signupresult

  val getNick: String => Task[Nick] =
    jsonString =>
      Parse.parseWith(
        jsonString,
        _
        .field("nick")
        .flatMap(_.string)
        .map(n => succ(Nick(n)))
        .getOrElse(fail("No field named 'nick' present in JSON")),
        fail
      )

  val checkNickTaken: Nick => ConnectionIO[Boolean] =
    nick =>
      PlayerDAO.getPlayerByNick(nick).map(_.nonEmpty)

  val createPlayer: (Nick, PlayerPreference) => ConnectionIO[Player] =
    (nick, playerPreference) => {
      val player = Player(PlayerId.randomId(), nick, playerPreference, Nil)
      PlayerDAO.insertPlayer(player).map(any => player)
    }

  val createPlayerIfNickAvailable: (Nick, PlayerPreference) => Task[Signupresult] =
    (nick, playerPreference) =>
      for {
        taken <- db.ds.transact(checkNickTaken(nick))
        rsult <-
        if (taken) Task.delay(NickTaken(nick))
        else db
             .ds
             .transact(createPlayer(nick, playerPreference))
             .map(SignupOk)
      } yield rsult


  val toResponse: Signupresult => Task[Response] = {
    case SignupOk(player) => Created(player.id.value)
    case NickTaken(nick) => Conflict(s"The nick ${nick.value} is taken, submit different nick")
  }

  val restApi: WebHandler = {
    case req@PUT -> Root / "player" / nick =>
      EntityDecoder.text(req)(body => {
        val maybePlayerPref =
          toJsonQuotes(body).decodeValidation[PlayerPreference]

        maybePlayerPref
        .fold(
            str => BadRequest(s"Failed to parse the preferences: $str, expected something like  {'drink':'wine','eat':'meat'}"),
            preference =>
              for {
                result <- createPlayerIfNickAvailable(Nick(nick), preference)
                response <- toResponse(result)
              } yield response
          )
      })
  }



}
