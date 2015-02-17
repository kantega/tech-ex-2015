package techex.domain

object notifications {

}

case class DeviceToken(value:String)
sealed trait NotificationTarget
case class Android() extends NotificationTarget
case class iOS(token:DeviceToken) extends NotificationTarget
case class Web() extends NotificationTarget
case class Slack() extends NotificationTarget
case class Device() extends NotificationTarget
case class SysOut() extends NotificationTarget

trait AttentionLevel{
  def asColor =
    this match{
      case Info => "good"
      case Attention => "warning"
      case Alert => "error"
      case _ => ""
    }
}
case object Info extends AttentionLevel
case object Green extends AttentionLevel
case object Attention extends AttentionLevel
case object Alert extends AttentionLevel
case class Notification(platform:NotificationTarget, message:String,severity:AttentionLevel = Info)
