package techex.domain

object notifications {

}

case class DeviceToken(value:String)
sealed trait NotificationTarget
case class Android() extends NotificationTarget
case class iOS(maybeToken:Option[DeviceToken]) extends NotificationTarget
case class Web() extends NotificationTarget
case class Slack() extends NotificationTarget
case class Device() extends NotificationTarget
case class SysOut() extends NotificationTarget

trait AttentionLevel{
  def asColor =
    this match{
      case Info => "#36a64f"
      case Good => "good"
      case Attention => "warning"
      case Alert => "danger"
      case _ => ""
    }
}
case object Info extends AttentionLevel
case object Good extends AttentionLevel
case object Attention extends AttentionLevel
case object Alert extends AttentionLevel
case class Notification(platform:NotificationTarget, message:String,severity:AttentionLevel = Info)
