package techex.cases

import java.util.concurrent.{ThreadFactory, Executors}

import techex.data.{slack, appleNotifications, PlayerData, Storage}

import scalaz._, Scalaz._
import doobie.util.process
import techex.domain._

import scalaz.concurrent.{Actor, Task}
import scalaz.stream.{process1, Process, Sink}

object notifyAboutUpdates {


  implicit val stateUpdateExecutor =
    Executors.newSingleThreadExecutor(new ThreadFactory {
      override def newThread(r: Runnable): Thread = {
        new Thread(r, "Notification thread")
      }
    })

  val factNotifcationProcess1 =
    process1.lift(factToNotifcation)

  lazy val factToNotifcation: Fact => List[Notification] = {
    case f: FactAboutPlayer           =>
      f match {
        case AwardedBadge(_, badge)       =>
          Notification(Slack(), ":star: " + f.player.player.nick.value + " was awarded the \"" + badge.achievement.name + "\" badge") ::
            Notification(SysOut() /*data.platform*/ , "Egentlig push notification til :" + f.player.platform + " You have been awarded the \"" + name + "\" badge") ::
            Nil
        case a@ArrivedAtArea(player, area) =>
          Notification(Slack(), ":walking: " + f.player.player.nick.value + " visited " + area.id) ::
            Notification(SysOut(), "Fact: " + f.player.player.nick.value + " visited " + area.id) ::
            Nil
        case any: FactAboutPlayer        =>
          Notification(SysOut(), "Fact: " + f.player.player.nick.value + " " + any) :: Nil
      }
    case scheduleEvent: ScheduleEvent => {
      Notification(SysOut(), "Scheduleupdate: " + scheduleEvent.toString) :: Nil
    }

    case any: Fact =>
      Notification(SysOut(), "Fact: " + any.toString) :: Nil
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

