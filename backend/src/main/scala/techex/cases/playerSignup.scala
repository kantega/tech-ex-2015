package techex.cases

import doobie.imports._
import techex._
import org.http4s._
import org.http4s.dsl._

import _root_.argonaut._
import Argonaut._
import org.http4s.argonaut._
import techex.data._
import techex.domain._

import scala.util.Random
import scalaz._, Scalaz._

import scalaz.concurrent.Task
import scalaz.stream.Sink
import scalaz.stream.async.mutable.Topic


object playerSignup {

  implicit def playerCodecJson: CodecJson[Player] =
    CodecJson(
      (p: Player) =>
        ("nick" := p.nick.value) ->:
          ("id" := p.id.value) ->:
          ("preferences" := p.preference) ->:
          ("quests" := p.privateQuests.map(_.id.value)) ->:
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
          privateQuests.map(str => quests.questMap(Qid(str))))
    )

  implicit val platformDecode: CodecJson[PlatformData] =
    casecodec2(PlatformData, PlatformData.unapply)("type", "deviceToken")

  implicit def playerPreferenceCode: CodecJson[PlayerPreference] =
    casecodec2(
      (drinkS: String, eatS: String) => PlayerPreference(Drink(drinkS), Eat(eatS)),
      (preference: PlayerPreference) => Some(preference.drink.asString, preference.eat.asString)
    )("drink", "eat")

  implicit val createPlayerDataDecode: CodecJson[CreatePlayerData] =
    casecodec2(CreatePlayerData, CreatePlayerData.unapply)("platform", "preferences")


  sealed trait Signupresult
  case class SignupOk(player: PlayerData) extends Signupresult
  case class NickTaken(nick: Nick) extends Signupresult

  val toFact: PartialFunction[InputMessage , State[Storage, List[Fact]]] = {
    case CreatePlayer(data) => State.state(List(PlayerCreated(data)))
  }

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

  val checkNickTaken: Nick => State[Storage, Boolean] =
    nick =>
      State.gets(_.playerData.exists(entry => entry._2.player.nick === nick))


  val createPlayer: (Nick, PlayerPreference, List[Quest]) => Player =
    (nick, playerPreference, personalQuests) => {
      Player(PlayerId.randomId(), nick, playerPreference, personalQuests)
    }

  def selectPersonalQuests(nick: Nick): List[Quest] = {
    val rand = Random
    rand.setSeed(nick.value.hashCode)
    val index =
      rand.nextInt(quests.questPermutations.length - 1)
    val perm =
      quests.questPermutations(index)

    List(perm._1, perm._2)
  }

  val updateContext: PlayerData => State[Storage, PlayerData] =
    playerData =>
      State(ctx =>
        (ctx.putPlayerData(playerData.player.id, playerData), playerData))


  val createPlayerIfNickAvailable: (Nick, CreatePlayerData) => State[Storage, Signupresult] =
    (nick, createData) =>
      for {
        taken <- checkNickTaken(nick)
        randomPersonQuests <- State.state(selectPersonalQuests(nick))
        player <- State.state(createPlayer(nick, createData.preferences.getOrElse(PlayerPreference.default), randomPersonQuests))
        rsult <- State.state(
          if (taken) NickTaken(nick)
          else SignupOk(PlayerData(
            player,
            Set(),
            Vector(),
            Vector(),
            player.privateQuests
              .map(q => quests.trackerForQuest.get(q.id))
              .collect { case Some(x) => x}
              .foldLeft(PatternOutput.zero[Achievement])(_ and _),
            createData.platform.toPlatform
          )))
      } yield rsult


  val toResponse: Signupresult => Task[Response] = {
    case SignupOk(player) => Created(player.player.asJson)
    case NickTaken(nick)  => Conflict(s"The nick ${nick.value} is taken, submit different nick")
  }

  def restApi(topic: Topic[InputMessage]): WebHandler = {
    case req@PUT -> Root / "player" / nick =>
      req.decode[String]{body => {
        val maybeCreatePlayerData =
          toJsonQuotes(body).decodeValidation[CreatePlayerData]

        maybeCreatePlayerData.fold(
          failMsg => BadRequest(failMsg),
          createPlayerData => {

            for {
              result <- Storage.run(createPlayerIfNickAvailable(Nick(nick), createPlayerData))
              _ <- result match {
                case ok@SignupOk(playerData) =>
                  Storage.run(updateContext(playerData)) *>
                    topic.publishOne(CreatePlayer(playerData))

                case _                       => Task {}
              }
              response <- toResponse(result)
            } yield response
          }
        )

      }}
  }

  case class PlatformData(plattformType: String, deviceToken: Option[String]) {
    def toPlatform =
      plattformType.toLowerCase match {
        case "ios"     => iOS(deviceToken.map(t => DeviceToken(t)))
        case "android" => Android()
        case _         => Web()
      }
  }
  case class CreatePlayerData(platform: PlatformData, preferences: Option[PlayerPreference])
  case class CreatePlayer(player: PlayerData) extends Command
}
