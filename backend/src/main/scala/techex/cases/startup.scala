package techex.cases

import java.util.concurrent.Executors

import com.notnoop.apns.APNS
import org.http4s.server._
import techex._
import techex.data._
import techex.domain._
import techex.web.test

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.stream._

object startup {
  val streamRunner =
    Executors.newSingleThreadScheduledExecutor()

  def setupStream: Task[Unit] = {

    val stream =
      eventstreams.events.subscribe pipe
        trackPlayer.handleTracking through
        PlayerStore.updates[List[FactUpdate]] pipe
        process1.unchunk[FactUpdate] through
        notifyAboutUpdates.factNotifcationChannel pipe
        process1.unchunk[Notification] to
        notifyAboutUpdates.notificationSink

    val scheduleStream =
      eventstreams.events.subscribe pipe
        updateSchedule.handleSchedulingProcess1 through
        Schedule.updates[List[ScheduleEvent]] pipe
        process1.unchunk[ScheduleEvent] through
        notifyAboutUpdates.scheduleupdateChannel pipe
        process1.unchunk[Notification] to
        notifyAboutUpdates.notificationSink

    Task {
      Task.fork(stream.onFailure(t => {
        t.printStackTrace()
        stream
      }).run)(streamRunner).runAsync(_.toString)
    }
  }


  def setup(cfg: Map[String, String]): Task[HttpService] = {

    val dbConfig =
      if (cfg.getOrElse("db", "mem") == "mysql")
        db.mysqlConfig(cfg.getOrElse("db.username", ""), cfg.getOrElse("db.password", ""))
      else
        db.inMemConfig

    for {
      _ <- notifyAboutUpdates.sendNotification(Notification(Slack(),"Starting up server", Attention))
      _ <- setupStream
      ds <- db.ds(dbConfig)
      _ <- ds.transact(PlayerDAO.create)
      _ <- Task.delay(println("Created player table"))
      _ <- ds.transact(ObservationDAO.createObservationtable)
      _ <- Task.delay(println("Created observation table"))

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
        endSession.restApi(eventstreams.events)
    )

  }
}
