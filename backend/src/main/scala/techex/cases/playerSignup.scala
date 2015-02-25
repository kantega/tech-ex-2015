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
import codecJson._

object playerSignup {




  sealed trait Signupresult
  case class SignupOk(player: PlayerData) extends Signupresult
  case class NickTaken(nick: Nick) extends Signupresult

  val toFact: PartialFunction[InputMessage , State[Storage, List[Fact]]] = {
    case CreatePlayer(data) => State{ctx =>
      val playerData =
        ctx.players.find(entry => entry.player.nick === data.nick)

      (ctx,List(PlayerCreated(playerData.get)))}
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


  val createPlayerIfNickAvailable: CreatePlayerData => State[Storage, Signupresult] =
     createData =>
      for {
        taken <- checkNickTaken(createData.nick)
        randomPersonQuests <- State.state(selectPersonalQuests(createData.nick))
        player <- State.state(createPlayer(createData.nick, createData.preferences.getOrElse(PlayerPreference.default), randomPersonQuests))
        rsult <- State.state(
          if (taken) NickTaken(createData.nick)
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
    case SignupOk(player) => Ok(player.player.asJson)
    case NickTaken(nick)  => Conflict(s"The nick ${nick.value} is taken, submit different nick")
  }

  def restApi(topic: Topic[InputMessage]): WebHandler = {
    case req@POST -> Root / "player"  =>
      req.decode[String]{body => {
        val maybeCreatePlayerData =
          toJsonQuotes(body).decodeValidation[CreatePlayerData]

        maybeCreatePlayerData.fold(
          failMsg => BadRequest(failMsg),
          createPlayerData => {

            for {
              result <- Storage.run(createPlayerIfNickAvailable(createPlayerData))
              _ <- result match {
                case ok@SignupOk(playerData) =>
                  Storage.run(updateContext(playerData)) *>
                    topic.publishOne(CreatePlayer(createPlayerData))

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
  case class CreatePlayerData(nick:Nick, platform: PlatformData, preferences: Option[PlayerPreference])

}
