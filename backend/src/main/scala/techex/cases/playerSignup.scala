package techex.cases

import _root_.argonaut._
import org.http4s._
import argonaut._,Argonaut._
import org.http4s.argonaut._
import org.http4s.dsl._
import org.joda.time.Instant
import techex._
import techex.data._
import techex.data.codecJson._
import techex.domain._

import scala.util.Random
import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task
import scalaz.stream.async.mutable.Topic

object playerSignup {




  sealed trait Signupresult
  case class SignupOk(player: PlayerData) extends Signupresult
  case class NickTaken(nick: Nick) extends Signupresult

  val toFact: PartialFunction[InputMessage , State[Storage, List[Fact]]] = {
    case CreatePlayer(data,instant,id) => State{ctx =>
      val playerData =
        ctx.playerData.get(id)

      playerData.fold((ctx,nil[PlayerCreated])){data =>
        (ctx,List(PlayerCreated(data,instant)))
      }

      }
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

  def selectPersonalQuests(venue:String,nick: Nick): List[Quest] = {
    venue match{
      case "kantega" => quests.kantegaQuests
      case _ => {
        val rand = Random
        rand.setSeed(nick.value.hashCode)
        val index =
          rand.nextInt(quests.questPermutations.length - 1)
        val perm =
          quests.questPermutations(index)
        List(perm._1, perm._2)
      }
    }
  }

  val updateContext: PlayerData => State[Storage, PlayerData] =
    playerData =>
      State(ctx =>
        (ctx.putPlayerData(playerData.player.id, playerData), playerData))


  def createPlayerIfNickAvailable(venue:String): CreatePlayerData => State[Storage, Signupresult] =
     createData =>
      for {
        taken <- checkNickTaken(createData.nick)
        randomPersonQuests <- State.state(selectPersonalQuests(venue,createData.nick))
        player <- State.state(createPlayer(createData.nick, createData.preferences.getOrElse(PlayerPreference.default), randomPersonQuests))
        rsult <- State.state(
          if (taken) NickTaken(createData.nick)
          else SignupOk(PlayerData(
            player,
            Set(),
            LocationUpdate(player.id,areas.somewhere,Instant.now()),
            player.privateQuests
              .map(q => quests.trackerForQuest.get(q.id))
              .collect { case Some(x) => x}
              .foldLeft(progresstracker.zero[Achievement])(_ and _),
            createData.platform.toPlatform
          )))
      } yield rsult


  val toResponse: Signupresult => Task[Response] = {
    case SignupOk(player) => Ok(player.player.asJson)
    case NickTaken(nick)  => Conflict(s"The nick ${nick.value} is taken, submit different nick")
  }

  def restApi(venue:String,topic: Topic[InputMessage]): WebHandler = {
    case req@POST -> Root / "players"  =>
      req.decode[String](body => {
        val maybeCreatePlayerData =
          toJsonQuotes(body).decodeValidation[CreatePlayerData]

        maybeCreatePlayerData.fold(
          failMsg => BadRequest(failMsg),
          createPlayerData => {

            for {
              result <- Storage.run(createPlayerIfNickAvailable(venue)(createPlayerData))
              _ <- result match {
                case ok@SignupOk(playerData) =>
                  Storage.run(updateContext(playerData)) *>
                    topic.publishOne(CreatePlayer(createPlayerData,Instant.now(),playerData.player.id))

                case _                       => Task {}
              }
              response <- toResponse(result)
            } yield response
          }
        )

      })
  }

  case class PlatformData(plattformType: String, deviceToken: Option[String]) {
    def toPlatform =
      plattformType.toLowerCase match {
        case "ios"     => iOS(deviceToken.map(t => DeviceToken(t)))
        case "android" => Android(deviceToken.map(t => DeviceToken(t)))
        case _         => Web()
      }
  }
  case class CreatePlayerData(nick:Nick, platform: PlatformData, preferences: Option[PlayerPreference])

}
