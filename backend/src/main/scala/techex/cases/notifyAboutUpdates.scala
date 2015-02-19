package techex.cases

import techex.data.{slack, appleNotifications, PlayerData, PlayerStore}

import scalaz._, Scalaz._
import doobie.util.process
import techex.domain._

import scalaz.concurrent.Task
import scalaz.stream.{Process, Sink}

object notifyAboutUpdates {

  val scheduleupdateChannel =
    Process.constant(scheduleupdateToNotifcation)

  lazy val scheduleupdateToNotifcation: ScheduleEvent => Task[List[Notification]] =
    update => {
      PlayerStore
        .run[PlayerStore](State.gets(ctx => ctx))
        .map(ctx => {
        Notification(Slack(), "Scheduleupdate: " + update.toString) :: Nil
      })
    }

  val factNotifcationChannel =
    Process.constant(factToNotifcation)

  lazy val factToNotifcation: FactUpdate => Task[List[Notification]] =
    update => {
      PlayerStore
        .run[PlayerData](State.gets(ctx => ctx.playerData(update.info.playerId)))
        .map(data => {
        update.fact match {
          case AchievedBadge(name) =>
            Notification(Slack(), ":star: " + data.player.nick.value + " was awarded the \"" + name + "\" badge") ::
              Notification(Slack() /*data.platform*/ , "Egentlig push notification til :" + data.platform + " You have been awarded the \"" + name + "\" badge") ::
              Nil
          case any: Fact           =>
            Notification(Slack(), "Fact: " + data.player.nick.value + " " + any.toString) :: Nil
        }
      })
    }

  lazy val sendNotification: Notification => Task[Unit] =
    notification => {
      notification.platform match {
        case Slack()                       => slack.sendMessage(notification.message, notification.severity.asColor)
        case iOS(token) if token.isDefined => appleNotifications.sendNotification(token.get, notification.message)
        case SysOut()                      => print(notification.severity.toString + " : " + notification.message)
        case platform: NotificationTarget  => print("Unspecified notification target:" + platform.toString + "-" + notification.message)
      }
    }

  lazy val notificationSink: Sink[Task, Notification] =
    process.sink(sendNotification)


  lazy val print: String => Task[Unit] =
    str => Task {
      println(str)
    }

}

