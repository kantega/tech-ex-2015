package techex.cases

import java.util.concurrent.Executors

import org.http4s.server._
import techex._
import streams._
import techex.data._
import techex.domain._
import techex.web.test

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.stream._

object startup {
  val streamRunner =
    namedSingleThreadExecutor("Streamsetup")

  def setupStream: Task[Unit] = {

    val handleInputQueue =
      async.unboundedQueue[InputMessage]

    val notificationQueue =
      async.unboundedQueue[Notification]


    val enqueueInputProcess =
      eventstreams.events.subscribe to handleInputQueue.enqueue

    val handleInputProcess =
      handleInputQueue.dequeue pipe
        process1.lift(trackPlayer.calcActivity orElse updateSchedule.handleScheduling orElse noOp) pipe
        appendAccumP1(calculatAchievements.calcAchievementsAndAwardBadges) through
        Storage.updates[List[Fact]] pipe
        process1.unchunk[Fact] pipe
        notifyAboutUpdates.factNotifcationProcess1 pipe
        process1.unchunk[Notification] to notificationQueue.enqueue

    val notificationProcess =
      notificationQueue.dequeue to notifyAboutUpdates.notificationSink


    Task {
      enqueueInputProcess.handle(streams.printAndReset(enqueueInputProcess)).run.runAsync(_.toString)
      handleInputProcess.handle(streams.printAndReset(handleInputProcess)).run.runAsync(_.toString)
      notificationProcess.handle(streams.printAndReset(notificationProcess)).run.runAsync(_.toString)
      println("Streams set up")
    }
  }

  def noOp:PartialFunction[InputMessage , State[Storage, List[Fact]]] = {
    case _=> State.state(List())
  }

  def setupSchedule: Task[Unit] = {
    val commands =
      scheduling.scheduleEntries.map(AddEntry).toSeq

    val t: Process[Task, Command] =
      Process.emitAll(commands)

    val tt =
      t to eventstreams.events.publish

    tt.run
  }

  def setup(cfg: Map[String, String]): Task[HttpService] = {

    val dbConfig =
      if (cfg.getOrElse("db", "mem") == "mysql")
        db.mysqlConfig(cfg.getOrElse("db.username", ""), cfg.getOrElse("db.password", ""))
      else
        db.inMemConfig

    for {
      _ <- notifyAboutUpdates.sendNotification(Notification(Slack(), "Starting up server", Attention))
      _ <- setupStream
      _ <- setupSchedule
    //ds <- db.ds(dbConfig)
    //_ <- ds.transact(PlayerDAO.create)
    //_ <- Task.delay(println("Created player table"))
    //_ <- ds.transact(ObservationDAO.createObservationtable)
    //_ <- Task.delay(println("Created observation table"))

    } yield HttpService(
      playerSignup.restApi(eventstreams.events) orElse
        test.testApi orElse
        listPersonalAchievements.restApi orElse
        listPersonalQuests.restApi orElse
        listTotalProgress.restApi orElse
        listTotalAchievements.restApi orElse
        trackPlayer.restApi(eventstreams.events) orElse
        unregisterPlayer.restApi orElse
        startSession.restApi(eventstreams.events) orElse
        endSession.restApi(eventstreams.events) orElse
        listSchedule.restApi
    )

  }
}
