package techex

import argonaut.Parse
import org.joda.time.Minutes._
import org.joda.time.Seconds._
import org.joda.time._
import org.specs2.mutable.Specification
import techex.TestServer._
import techex.cases.playerSignup
import techex.cases.playerSignup.{SignupOk, PlatformData, CreatePlayerData}
import techex.data._
import techex.domain.{Far, Web, Near, Nick}
import techex.domain.areas._

import scalaz.concurrent.Task

class MessagesSpec extends Specification {

  val runningserver =
    server.run


  "When submitting events" should {
    "The server should report achievemnts and badges" in {

      Thread.sleep(1000)

      val createPlayerData =
        CreatePlayerData(Nick("jalle"), PlatformData("any", None), None)

      val T = new DateTime(2015, 3, 5, 7, 0).toInstant

      val task = for {
        result <- Storage.run(playerSignup.createPlayerIfNickAvailable(createPlayerData))
        data <- result match {
          case SignupOk(playerData) => Task(playerData)
        }
        _ <- Storage.run(playerSignup.updateContext(data))
        _ <- eventstreams.events.publishOne(CreatePlayer(createPlayerData,T))
        _ <- eventstreams.events.publishOne(EnterObservation(beaconAt(kantegaCoffeeDn), data.player.id, T, Far))
        _ <- eventstreams.events.publishOne(EnterObservation(beaconAt(kantegaCoffeeDn), data.player.id, T.plus(seconds(5)), Near))
        _ <- eventstreams.events.publishOne(ExitObservation(data.player.id, T.plus(seconds(10))))

        _ <- eventstreams.events.publishOne(EnterObservation(beaconAt(kantegaCoffeeDn), data.player.id, T.plus(minutes(10)), Far))
        _ <- eventstreams.events.publishOne(EnterObservation(beaconAt(kantegaCoffeeDn), data.player.id, T.plus(minutes(11)), Near))
        _ <- eventstreams.events.publishOne(ExitObservation(data.player.id, T.plus(minutes(12))))

        _ <- eventstreams.events.publishOne(EnterObservation(beaconAt(kantegaCoffeeUp), data.player.id, T.plus(minutes(13)), Near))
        _ <- eventstreams.events.publishOne(ExitObservation(data.player.id, T.plus(minutes(14))))

        _ <- eventstreams.events.publishOne(EnterObservation(beaconAt(desk1), data.player.id, T.plus(minutes(16)), Near))

        _ <- eventstreams.events.publishOne(EnterObservation(beaconAt(desk2), data.player.id, T.plus(minutes(18)), Near))

      } yield ()

      val rsult =
        task.attemptRun
      Thread.sleep(10000)
      rsult.toString ! (rsult.isRight must_== true)
    }

  }
  runningserver.shutdown


  implicit def toDuration(n: ReadablePeriod): ReadableDuration = n.toPeriod.toStandardDuration
}
