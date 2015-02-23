package techex.domain

import org.joda.time.{DateTime, Duration}
import techex.data.{Command, PlayerData}

object facts {

}

trait Fact
trait FactAboutPlayer extends Fact {
  val player: PlayerData
}
case class JoinedActivity(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class LeftActivity(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class JoinedOnTime(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class LeftOnTime(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class ArrivedAtArea(player: PlayerData, area: Area) extends FactAboutPlayer
case class LeftArea(player: PlayerData, area: Area) extends FactAboutPlayer
case class Attended(player: PlayerData, event: ScheduleEntry) extends FactAboutPlayer
case class CameEarly(player: PlayerData, event: ScheduleEntry, duration: Duration) extends FactAboutPlayer
case class CameLate(player: PlayerData, event: ScheduleEntry, duration: Duration) extends FactAboutPlayer
case class LeftEarly(player: PlayerData, event: ScheduleEntry, duration: Duration, cause: String) extends FactAboutPlayer
case class LeftFor(player: PlayerData, event: ScheduleEntry, activity: String, duration: Duration) extends FactAboutPlayer
case class MetPlayer(player: PlayerData, otherPlayer: PlayerData) extends FactAboutPlayer
case class EarnedAchievemnt(player: PlayerData, achievemnt:Achievement) extends FactAboutPlayer
case class AwardedBadge(player: PlayerData, badge:Badge) extends FactAboutPlayer

trait ScheduleEvent extends Fact
case class Started(instant: DateTime, entry: ScheduleEntry) extends ScheduleEvent
case class Ended(instant: DateTime, entry: ScheduleEntry) extends ScheduleEvent
case class Added(entry: ScheduleEntry) extends ScheduleEvent
case class Removed(entry: ScheduleEntry) extends ScheduleEvent
case class StartEntry(entryId: ScId) extends Command
case class EndEntry(entryId: ScId) extends Command
case class AddEntry(entry: ScheduleEntry) extends Command
case class RemoveEntry(entryId: ScId) extends Command