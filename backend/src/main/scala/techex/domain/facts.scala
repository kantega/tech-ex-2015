package techex.domain

import org.joda.time.{DateTime, Duration}
import techex.data.{Command, PlayerData}

object facts {

}

trait Fact
trait FactAboutPlayer extends Fact {
  val player: PlayerData
}
case class JoinedActivityLate(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class LeftActivityEarly(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class JoinedOnStart(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class LeftOnEnd(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class ArrivedAtArea(player: PlayerData, area: Area) extends FactAboutPlayer
case class LeftArea(player: PlayerData, area: Area) extends FactAboutPlayer
case class MetPlayer(player: PlayerData, otherPlayer: PlayerData) extends FactAboutPlayer
case class EarnedAchievemnt(player: PlayerData, achievemnt:Achievement) extends FactAboutPlayer
case class AwardedBadge(player: PlayerData, badge:Badge) extends FactAboutPlayer
case class PlayerCreated(player:PlayerData) extends FactAboutPlayer
trait ScheduleEvent extends Fact
case class Started(instant: DateTime, entry: ScheduleEntry) extends ScheduleEvent
case class Ended(instant: DateTime, entry: ScheduleEntry) extends ScheduleEvent
case class Added(entry: ScheduleEntry) extends ScheduleEvent
case class Removed(entry: ScheduleEntry) extends ScheduleEvent
