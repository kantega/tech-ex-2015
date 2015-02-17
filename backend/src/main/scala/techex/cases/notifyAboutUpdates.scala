package techex.cases

import scalaz._, Scalaz._
import doobie.util.process
import techex.domain.{ScheduleEvent, Fact, AchievedBadge, FactUpdate}

import scalaz.concurrent.Task
import scalaz.stream.Sink

object notifyAboutUpdates {

  lazy val notifyScheduleChanges:  ScheduleEvent => Task[Unit] =
    evt =>  sendMessageToSlack("Schedule update: "+evt.toString)

  lazy val notifyScheduleChangesSink: Sink[Task, ScheduleEvent] =
    process.sink(notifyScheduleChanges)

  lazy val notifyUpdate: FactUpdate => Task[Unit] =
    update => update.fact match {
      case AchievedBadge(name) =>
        sendMessageToSlack(":star: " + update.info.nick.value + " was awarded the \"" + name+"\" badge") *>
          print(update.toString)
      case any:Fact                 =>
        sendMessageToSlack("Fact: "+update.info.nick.value+" "+any.toString)
    }

  lazy val notifyUpdateSink: Sink[Task, FactUpdate] =
    process.sink(notifyUpdate)

  lazy val notifyMessage: (String, String) => Task[Unit] =
    (msg, color) => sendMessageToSlack(msg, color)

  lazy val notifyMessageWithDefaultColor: String => Task[Unit] =
    msg => sendMessageToSlack(msg)

  lazy val print: String => Task[Unit] =
    str => Task {println(str)}

  lazy val printSink: Sink[Task, String] =
    process.sink(print) //sendMessage(msg))

  def sendMessageToSlack(msg: String, color: String) =
    techex.data.slack.sendMessage(msg, color)

  def sendMessageToSlack(msg: String) =
    techex.data.slack.sendMessage(msg)

  lazy val slackSink: Sink[Task, String] =
    process.sink((msg: String) => sendMessageToSlack(msg))
}

