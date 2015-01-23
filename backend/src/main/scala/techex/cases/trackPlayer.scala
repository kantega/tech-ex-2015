package techex.cases

import java.util.UUID

import org.joda.time.Instant
import techex.data._
import techex.domain._

import scalaz._, Scalaz._
import scalaz.stream._
import scalaz.concurrent.Task

object trackPlayer {

  import tracking._

  val toMovement: Process1[(Observation, List[Location]), (Option[Location], List[Location])] = process1.lift {
    case (observation, history) =>{

      val maybeArea =
        observation.beacon.flatMap(areas.beaconPlacement.get)

      (maybeArea, history) match {
        case (None, Nil)                       => (None, Nil)
        case (None, hist)                      => (None, hist)
        case (Some(area), Nil)               => (Some(Location(UUID.randomUUID(), observation.playerId, area, Instant.now())), Nil)
        case (Some(area), list@last :: rest) =>
          if (area === last.area) (None, list)
          else (Some(Location(UUID.randomUUID(), observation.playerId, area, Instant.now())), list)
      }}
  }

  val loadHistory: Channel[Task, Observation, (Observation, List[Location])] =
    Process.constant {
      observation =>
        db.ds.transact(LocationDao.loadLocationsForPlayer(observation.playerId, 20)).map(list => (observation, list))
    }

  val saveHistory: Channel[Task, (Option[Location], List[Location]), (Option[Location], List[Location])] = {
    Process.constant {
      case (None, history)           => Task.now((None, history))
      case (Some(location), history) => db.ds.transact(LocationDao.storeLocation(location)).map(int => (Some(location), history))
    }
  }

  val prepend: Process1[(Option[Location], List[Location]), List[Location]] = process1.lift({
    case (None, list)           => list
    case (Some(location), list) => location :: list
  })


  def loadPlayer(playerId: PlayerId): Task[Option[Player]] = {
    db.ds.transact(PlayerDAO.getPlayerById(playerId))
  }


  lazy val trackPlayerPipeline: Process[Task, List[Location]] =
    eventstreams.observations.subscribe through
      loadHistory pipe
      toMovement through
      saveHistory pipe
      prepend

}
