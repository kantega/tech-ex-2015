package techex.cases

import java.util.concurrent.TimeUnit

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
  val scheduler = DefaultScheduler

  def setupScheduleEvents(topic: Topic[StreamEvent]): Task[Unit] = Task {
    // imagine an asynchronous task which eventually produces an `Int`
    val now =
      DateTime.now()

    val tasks =
      for {
        entry <- schedule.scheduleEntries
        event <- eventsForEntry(entry)
      } yield (durationBetween(now, event.instant), event)

    tasks.foreach { case (delay, task) =>
      scheduler.schedule(new Runnable() {
        override def run(): Unit =
          try {
            topic.publishOne(task)
          } catch {
            case t: Throwable => t.printStackTrace()
          }
      }, delay.getMillis, TimeUnit.MILLISECONDS)
    }
  }


  def eventsForEntry(entry: ScheduleEntry): List[ScheduleEvent] = {
    List(
      ScheduleEvent(entry.time.start, entry, Start),
      ScheduleEvent(entry.time.start.plus(entry.time.duration), entry, End)
    )
  }

  /*
    def loadPlayer(playerId: PlayerId): Task[Option[Player]] = {
      db.ds.transact(PlayerDAO.getPlayerById(playerId))
    }

    val loadHistory: Channel[Task, Observation, (Observation, List[LocationUpdate])] =
      Process.constant {
        observation =>
          db.ds.transact(LocationDao.loadLocationsForPlayer(observation.playerId, 20)).map(list => (observation, list))
      }

    val saveHistory: Channel[Task, (Option[LocationUpdate], List[LocationUpdate]), (Option[LocationUpdate], List[LocationUpdate])] = {
      Process.constant {
        case (None, history)           => Task.now((None, history))
        case (Some(location), history) => db.ds.transact(LocationDao.storeLocation(location)).map(int => (Some(location), history))
      }
    }
  */


  def setup(cfg: Map[String,String]): Task[HttpService] = {

    val dbConfig =
      if (cfg.getOrElse("db","mem") == "mysql")
        db.mysqlConfig(cfg.getOrElse("db.username",""), cfg.getOrElse("db.password",""))
      else
        db.inMemConfig

    for {
      _ <- notifyAdmin.sendMessage("Starting up server","warning")
      ds <- db.ds(dbConfig)
      _ <- ds.transact(PlayerDAO.create)
      _ <- Task.delay(println("Created player table"))
      _ <- ds.transact(ObservationDAO.createObservationtable)
      _ <- Task.delay(println("Created observation table"))
      _ <- setupScheduleEvents(eventstreams.events)

    } yield HttpService(
      playerSignup.restApi(ds) orElse
        test.testApi orElse
        listPersonalAchievements.restApi orElse
        listPersonalQuests.restApi orElse
        listTotalProgress.restApi orElse
        listTotalAchievements.restApi orElse
        trackPlayer.restApi orElse
    unregisterPlayer.restApi)

  }
}
