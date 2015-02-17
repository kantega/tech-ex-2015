package techex.cases

import doobie.imports._
import techex._
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut.ArgonautSupport._
import techex.data._
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
        _.field("nick")
          .flatMap(_.string)
          .map(n => succ(Nick(n)))
          .getOrElse(fail("No field named 'nick' present in JSON")),
        fail
      )

  val checkNickTaken: Nick => State[PlayerStore, Boolean] =
    nick =>
      PlayerStore.read(_.playerData.exists(entry => entry._2.player.nick === nick))


  val createPlayer: (Nick, PlayerPreference, List[QuestId]) => Player =
    (nick, playerPreference, personalQuests) => {
      Player(PlayerId.randomId(), nick, playerPreference, personalQuests)
    }

  val selectPersonalQuests: List[QuestId] =
    quests.quests.map(q => QuestId(q.id.value))

  val updateContext: Player => State[PlayerStore, Player] =
    player =>
      State[PlayerStore, Player](ctx =>
        (ctx.putPlayerData(
          player.id,
          PlayerData(
            player,
            Set(),
            Vector(),
            Vector(),
            player.privateQuests
              .map(id => Qid(id.value))
              .map(id => quests.trackerForQuest.get(id))
              .collect { case Some(x) => x}
              .foldLeft(PatternOutput.zero[Badge])(_ and _)
          )),
          player)
      )


  val createPlayerIfNickAvailable: (Nick, PlayerPreference) => State[PlayerStore, Signupresult] =
    (nick, playerPreference) =>
      for {
        taken <- checkNickTaken(nick)
        randomPersonQuests <- State.state(selectPersonalQuests)
        player <- State.state(createPlayer(nick, playerPreference, randomPersonQuests))
        rsult <- State.state(
          if (taken) NickTaken(nick)
          else SignupOk(player))
      } yield rsult


  val toResponse: Signupresult => Task[Response] = {
    case SignupOk(player) => Created(player.asJson)
    case NickTaken(nick)  => Conflict(s"The nick ${nick.value} is taken, submit different nick")
  }

  def restApi: WebHandler = {
    case req@PUT -> Root / "player" / nick =>
      EntityDecoder.text(req)(body => {
        val maybePlayerPref =
          toJsonQuotes(body).decodeValidation[PlayerPreference]

        val preference =
          maybePlayerPref.getOrElse(PlayerPreference.default)

        for {
          result <- PlayerStore.run(createPlayerIfNickAvailable(Nick(nick), preference))
          _ <- result match {
            case ok@SignupOk(player) => PlayerStore.run(updateContext(player)) *> notifyAboutUpdates.notifyMessageWithDefaultColor("Player " + nick +" jsut signed up! :thumbsup:")
            case _                   => Task {}
          }
          response <- toResponse(result)
        } yield response

      })
  }

}
