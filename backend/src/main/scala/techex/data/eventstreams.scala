package techex.data

import org.joda.time.Instant
import techex.cases.playerSignup.CreatePlayerData
import techex.domain._

import scalaz.stream.async
import scalaz.stream.async.mutable.Topic

object eventstreams {


  lazy val events: Topic[InputMessage] =
    scalaz.stream.async.topic()

  val factUdpates: Topic[Fact] =
    async.topic()

}

trait InputMessage{
  val msgType:String
}
trait Command extends InputMessage
case class Observation(beacon: Beacon, playerId: PlayerId, instant: Instant, proximity: Proximity) extends InputMessage{
  val msgType = "Observation"
}
case class CreatePlayer(data:CreatePlayerData) extends Command{
  val msgType = "CreatePlayer"
}

case class StartEntry(entryId: ScId) extends Command{val msgType="StartEntry"}
case class EndEntry(entryId: ScId) extends Command{val msgType="EndEntry"}
case class AddEntry(entry: ScheduleEntry) extends Command{val msgType="AddEntry"}
case class RemoveEntry(entryId: ScId) extends Command{val msgType="RemoveEntry"}