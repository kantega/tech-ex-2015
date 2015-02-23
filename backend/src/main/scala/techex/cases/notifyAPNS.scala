package techex.cases

import doobie.util.process
import techex.domain._
import techex.streams

import scalaz.concurrent.Task
import scalaz.stream.{Process, Sink}

object notifyAPNS {
  val notificationQueue =
    scalaz.stream.async.unboundedQueue[Fact]

  def setup(facts:Process[Task,Fact]): Task[Unit] =
    Task {
      (facts to notificationQueue.enqueue).run.runAsync(println(_))
      (notificationQueue.dequeue to sender)
        .handle(streams.printAndReset(notificationQueue.dequeue to sender))
        .run.runAsync(println(_))
    }

  def sender: Sink[Task, Fact] =
    process.sink(handleFact)


  def handleFact: Fact => Task[Unit] = {
    case AwardedBadge(playerData, badge) => playerData.platform match{
      case iOS(Some(token)) => Task{println("Egentlig push til APNS: "+ "You have been awarded the "+badge.achievement.name+" badge")}
      //appleNotifications.sendNotification(token, "You have been awarded the "+badge.achievement.name+" badge")
      case _ => Task{}
    }

    case any: Fact          => Task {}
  }
}
