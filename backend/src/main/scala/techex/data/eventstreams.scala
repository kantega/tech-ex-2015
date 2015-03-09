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
  val instant:Instant
}
trait Command extends InputMessage
case class EnterObservation(beacon: BeaconId, playerId: PlayerId, instant: Instant, proximity: Proximity) extends InputMessage{
  val msgType = "EnterObservation"
}
case class ExitObservation(playerId:PlayerId,instant:Instant) extends InputMessage{
  val msgType = "ExitObservation"
}
case class CreatePlayer(data:CreatePlayerData,instant:Instant) extends Command{
  val msgType = "CreatePlayer"
}

case class StartEntry(entryId: ScId,instant:Instant) extends Command{val msgType="StartEntry"}
case class EndEntry(entryId: ScId,instant:Instant) extends Command{val msgType="EndEntry"}
case class AddEntry(entry: ScheduleEntry,instant:Instant) extends Command{
  val msgType="AddEntry"
}
case class RemoveEntry(entryId: ScId,instant:Instant) extends Command{val msgType="RemoveEntry"}