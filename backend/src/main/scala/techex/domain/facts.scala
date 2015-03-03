package techex.domain

import org.joda.time.{Instant, DateTime, Duration}
import techex.data.{Command, PlayerData}

object facts {

}

trait Fact {
  val instant: Instant
}
trait FactAboutPlayer extends Fact {
  val player: PlayerData

}
case class JoinedActivityLate(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class LeftActivityEarly(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class JoinedOnStart(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class LeftOnEnd(player: PlayerData, event: ScheduleEntry, instant: Instant) extends FactAboutPlayer
case class EnteredRegion(player: PlayerData, area: Region, instant: Instant) extends FactAboutPlayer
case class LeftRegion(player: PlayerData, area: Region, instant: Instant) extends FactAboutPlayer
case class MetPlayer(player: PlayerData, otherPlayer: PlayerData, instant: Instant) extends FactAboutPlayer
case class EarnedAchievemnt(player: PlayerData, achievemnt: Achievement, instant: Instant) extends FactAboutPlayer
case class AwardedBadge(player: PlayerData, badge: Badge, instant: Instant) extends FactAboutPlayer
case class PlayerCreated(player: PlayerData, instant: Instant) extends FactAboutPlayer
trait ScheduleEvent extends Fact
case class Started(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent
case class Ended(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent
case class Added(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent
case class Removed(entry: ScheduleEntry, instant: Instant) extends ScheduleEvent

case class StartOfDay(instant:Instant) extends Fact
case class EndOfDay(instant:Instant) extends Fact
