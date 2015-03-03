package techex.cases

import doobie.util.process
import techex.data.{Observation, slack}
import techex.domain._
import techex.streams

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task
import scalaz.stream.{Process, Sink}

object notifySlack {

  val notificationQueue =
    scalaz.stream.async.unboundedQueue[AnyRef]

  def setup(facts: Process[Task, AnyRef]): Task[Unit] =
    Task {
      (facts to notificationQueue.enqueue).run.runAsync(println(_))
      (notificationQueue.dequeue to sender)
        .handle(streams.printAndReset(notificationQueue.dequeue to sender))
        .run.runAsync(println(_))
    }

  def sender: Sink[Task, AnyRef] =
    process.sink(handleFact)


  def handleFact: AnyRef => Task[Unit] = {
    case AwardedBadge(player, badge, _)              =>
      slack.sendMessage(":star: *" + player.player.nick.value + "* was awarded the _" + badge.achievement.name + "_ badge", Good)
    case EnteredRegion(player, area, _)              =>
      slack.sendMessage("*" + player.player.nick.value + "* visited _" + area.name + "_")
    case PlayerCreated(player, _)                    =>
      slack.sendMessage(":thumbsup: *" + player.player.nick.value + "* just signed up with quests _" + player.player.privateQuests.map(_.name).mkString("_ and _") + "_", Good)
    case JoinedActivityLate(player, event, _)        =>
      slack.sendMessage(":thumbsdown: *" + player.player.nick.value + "* came _late_ for _" + event.name + "_")
    case JoinedOnStart(player, event, _)             =>
      slack.sendMessage(":thumbsup: *" + player.player.nick.value + "* came _early_ for _" + event.name + "_")
    case Observation(beacon, playerId, _, proximity) =>
      slack.sendMessage(playerId.value + " is " + proximity.asString + " to " + beacon.id + " ")
    case any: AnyRef                                 => Task {}
  }
}
