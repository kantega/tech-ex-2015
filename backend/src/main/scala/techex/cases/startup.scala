package techex.cases

import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.config.Config
import org.http4s.server._
import org.joda.time.DateTime
import techex._
import techex.data._
import techex.domain._
import techex.web.test

import scalaz.concurrent.Task
import scalaz.stream._
import scalaz.stream.async.mutable.Topic

object startup {
  val streamRunner = Executors.newSingleThreadScheduledExecutor()

  def setupStream: Task[Unit] = {

    val stream =
      eventstreams.events.subscribe pipe
        trackPlayer.handleTracking through
        PlayerStore.updates[List[FactUpdate]] pipe
        process1.id.flatMap((list: List[FactUpdate]) => Process.emitAll(list.toSeq)) to
        notifyAboutUpdates.notifyUpdateSink


    Task{Task.fork(stream.onFailure(t=>{
      t.printStackTrace()
      stream
    }).run)(streamRunner).runAsync(_.toString)}
  }


  def setup(cfg: Map[String, String]): Task[HttpService] = {

    val dbConfig =
      if (cfg.getOrElse("db", "mem") == "mysql")
        db.mysqlConfig(cfg.getOrElse("db.username", ""), cfg.getOrElse("db.password", ""))
      else
        db.inMemConfig

    for {
      _ <- notifyAboutUpdates.notifyMessage("Starting up server", "warning")
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
