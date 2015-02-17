package techex.domain

import org.joda.time.Duration

object facts {

}

trait Fact
case class JoinedActivity(event: ScheduleEntry) extends Fact
case class LeftActivity(event: ScheduleEntry) extends Fact
case class JoinedOnTime(event: ScheduleEntry) extends Fact
case class LeftOnTime(event: ScheduleEntry) extends Fact
case class Entered(area: Area) extends Fact
case class LeftArea(area: Area) extends Fact
trait AggregatedFact extends Fact
case class Attended(event: ScheduleEntry) extends AggregatedFact
case class CameEarly(event: ScheduleEntry, duration: Duration) extends AggregatedFact
case class CameLate(event: ScheduleEntry, duration: Duration) extends AggregatedFact
case class LeftEarly(event: ScheduleEntry, duration: Duration, cause: String) extends AggregatedFact
case class LeftFor(event: ScheduleEntry, activity: String, duration: Duration) extends AggregatedFact
case class MetPlayer(playerId: PlayerId,nick:Nick) extends AggregatedFact
case class AchievedBadge(name: String) extends AggregatedFact