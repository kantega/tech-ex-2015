package techex.cases

import java.util.concurrent.Executors

import com.typesafe.config.Config
import doobie.util.process
import doobie.util.transactor.Transactor
import org.http4s.server._
import org.joda.time.Instant
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


  val messagehandlerPipe =
    process1.lift(trackPlayer.calcActivity orElse updateSchedule.handleScheduling orElse playerSignup.toFact orElse noOp) pipe
      appendAccumP1(locateOnSessionTimeBoundaries.handleTimeBoundsFacts) pipe
      appendAccumP1(calculatAchievements.calcAchievementsAndAwardBadges)

  def setupStream(txor: Transactor[Task]): Task[Unit] = {

    val handleTicks =
      produceTicks.days to eventstreams.events.publish

    val inputHandlerQueue =
      async.unboundedQueue[InputMessage]

    val storeToDatabaseQueue =
      async.unboundedQueue[InputMessage]

    val enqueueToInputHandlerProcess =
      eventstreams.events.subscribe to inputHandlerQueue.enqueue

    val enqeueToDatabaseQueueProcess =
      eventstreams.events.subscribe to storeToDatabaseQueue.enqueue

    val handleStoreToDatabase =
      storeToDatabaseQueue.dequeue to
        process.sink[Task, InputMessage]((input: InputMessage) => {
          txor.transact(InputMessageDAO.storeObservation(input))
        })

    val handleInputStream =
      inputHandlerQueue.dequeue pipe
        messagehandlerPipe through
        Storage.updates[List[Fact]] pipe
        process1.unchunk[Fact] to eventstreams.factUdpates.publish


    Task {
      handleTicks.run.runAsync(_.toString)
      notifyGCM.setup(eventstreams.factUdpates.subscribe).runAsync(_.toString)
      notifySlack.setup(eventstreams.factUdpates.subscribe).runAsync(_.toString)
      printFactsToLog.setup(eventstreams.factUdpates.subscribe).runAsync(_.toString)
      notifyAPNS.setup(eventstreams.factUdpates.subscribe).runAsync(_.toString)
      enqueueToInputHandlerProcess.handle(streams.printAndReset(enqueueToInputHandlerProcess)).run.runAsync(_.toString)
      handleInputStream.handle(streams.printAndReset(handleInputStream)).run.runAsync(_.toString)
      enqeueToDatabaseQueueProcess.handle(streams.printAndReset(enqeueToDatabaseQueueProcess)).run.runAsync(_.toString)
      handleStoreToDatabase.handle(streams.printAndReset(handleStoreToDatabase)).run.runAsync(_.toString)
      println("Streams set up")
    }
  }

  def noOp: PartialFunction[InputMessage, State[Storage, List[Fact]]] = {
    case m: Fact => State.state(List(m))
    case _       => State.state(List())
  }

  def setupSchedule: Task[Unit] = {
    val commands =
      scheduling.scheduleEntries.map(entry => AddEntry(entry,Instant.now())).toSeq

    val t: Process[Task, Command] =
      Process.emitAll(commands)

    val tt =
      t to eventstreams.events.publish

    tt.run
  }

  def setup(cfg: Config): Task[(HttpService, WSHandler)] = {

    val dbConfig =
      if (getStringOr(cfg, "db_type", "mem") == "mysql")
        db.mysqlConfig(cfg.getString("db_username"), cfg.getString("db_password"))
      else
        db.inMemConfig

    for {
      _ <- slack.sendMessage("Starting up server with " + getStringOr(cfg, "db_type", "mem"), Attention)
      ds <- db.ds(dbConfig)
      _ <- ds.transact(InputMessageDAO.createObservationtable)
      _ <- setupStream(ds)
      _ <- setupSchedule

    } yield (HttpService(
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
        listSchedule.restApi orElse
        serveHelptext.restApi orElse
        getBeaconRegions.restApi orElse
        getAreas.restApi
    ), getUpdateStream.wsApi(eventstreams.factUdpates))

  }
}
