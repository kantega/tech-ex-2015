package techex.domain

import org.joda.time.{ReadableInstant, Instant, DateTime, Duration}
import techex.data.{InputMessage, Command, PlayerData}

object facts {

}

trait Fact {
  val instant: Instant
}
object Fact {
  implicit def toReadableInstant[A <: Fact]: A => ReadableInstant = fact => fact.instant
}
trait FactAboutPlayer extends Fact {
  val player: PlayerData

}
case class JoinedActivityLate(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class LeftActivityEarly(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class JoinedOnStart(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class LeftOnEnd(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class EnteredArea(player: PlayerData, area: Area, instant: Instant) extends FactAboutPlayer
case class LeftArea(player: PlayerData, area: Area, instant: Instant) extends FactAboutPlayer
case class AtArea(player:PlayerData,area:Area,instant:Instant,duration:Duration) extends FactAboutPlayer
case class MetPlayer(player: PlayerData, otherPlayer: PlayerData, instant: Instant) extends FactAboutPlayer
case class EarnedAchievemnt(player: PlayerData, achievemnt: Achievement, instant: Instant) extends FactAboutPlayer
case class AwardedBadge(player: PlayerData, badge: Badge, instant: Instant) extends FactAboutPlayer
case class PlayerCreated(player: PlayerData, instant: Instant) extends FactAboutPlayer
trait ScheduleEvent extends Fact
case class Started(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent
case class Ended(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent
case class Added(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent
case class Removed(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent
trait Ticks extends Fact with InputMessage
case class StartOfDay(instant: Instant) extends Ticks {
  override val msgType: String = "StartOfDay"
}
case class EndOfDay(instant: Instant) extends Ticks {
  override val msgType: String = "EndOfDay"
}
case class StartOfTenSecs(instant:Instant) extends Ticks{
  override val msgType:String = "StartOfTenSecs"
}

case class EndOfTenSecs(instant:Instant) extends Ticks{
  override val msgType:String = "EndOfTenSecs"
}
