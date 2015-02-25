package techex.cases

import doobie.util.process
import techex.data.slack
import techex.domain._
import techex.streams

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task
import scalaz.stream.{Process, Sink}

object notifySlack {

  val notificationQueue =
    scalaz.stream.async.unboundedQueue[Fact]

  def setup(facts: Process[Task, Fact]): Task[Unit] =
    Task {
      (facts to notificationQueue.enqueue).run.runAsync(println(_))
      (notificationQueue.dequeue to sender)
        .handle(streams.printAndReset(notificationQueue.dequeue to sender))
        .run.runAsync(println(_))
    }

  def sender: Sink[Task, Fact] =
    process.sink(handleFact)


  def handleFact: Fact => Task[Unit] = {
    case AwardedBadge(player, badge)        =>
      slack.sendMessage(":star: *" + player.player.nick.value + "* was awarded the _" + badge.achievement.name + "_ badge", Good)
    case ArrivedAtArea(player, area) =>
      slack.sendMessage("*" + player.player.nick.value + "* visited _" + area.id +"_")
    case PlayerCreated(player) =>
      slack.sendMessage(":thumbsup: *" + player.player.nick.value + "* just signed up with quests _" + player.player.privateQuests.map(_.name).mkString("_ and _") + "_", Good)
    case JoinedActivityLate(player,event) =>
      slack.sendMessage(":thumbsdown: *" + player.player.nick.value + "* came _late_ for _" + event.name+ "_")
    case JoinedOnStart(player,event) =>
      slack.sendMessage(":thumbsup: *" + player.player.nick.value + "* came _early_ for _" + event.name+ "_")
    case any: Fact                     => Task {}
  }
}
