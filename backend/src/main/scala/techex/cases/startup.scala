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

    val q = async.unboundedQueue[Notification]


    val stream =
      eventstreams.events.subscribe pipe
        process1.lift(trackPlayer.calcActivity orElse updateSchedule.handleScheduling) pipe
        appendAccumP1(calculatAchievements.calcAchievementsAndAwardBadges) through
        Storage.updates[List[Fact]] pipe
        process1.unchunk[Fact] pipe
        notifyAboutUpdates.factNotifcationProcess1 pipe
        process1.unchunk[Notification] to q.enqueue

    val notificationStream =
      q.dequeue to notifyAboutUpdates.notificationSink



    Task {
      Task.fork(stream.onFailure(t => {
        t.printStackTrace()
        stream
      }).run)(streamRunner).runAsync(_.toString)

      Task.fork(notificationStream.onFailure(t => {
        t.printStackTrace()
        notificationStream
      }).run)(streamRunner).runAsync(_.toString)
    }
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
      playerSignup.restApi orElse
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
