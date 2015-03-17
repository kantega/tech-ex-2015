package techex.data

import techex._
import techex.domain._

import scalaz.Scalaz._
import scalaz.{State, _}
import scalaz.concurrent.Task
import scalaz.stream.{Channel, Process}

object Storage {


  val historySize = 100

  implicit val stateUpdateExecutor =
    namedSingleThreadExecutor("Stateupdater")

  private var playerContext: Storage =
    Storage(Map(), Map())


  def run[A](state: State[Storage, A]): Task[A] = {
    Task {
      val (ctx, a) = state(playerContext)
      playerContext = ctx
      a
    }(stateUpdateExecutor)
  }


  def updates[A]: Channel[Task, State[Storage, A], A] =
    Process.constant(run)

  def playersPresentAt(area: Area): State[Storage, List[PlayerData]] = {
    State.gets { ctx =>
      ctx.playersPresentAt(area)
    }
  }

  def updatePlayer: PlayerId => (PlayerData => PlayerData) => State[Storage, Unit] =
    id => f => State { ctx =>
      (ctx.updatePlayerData(id, f), Unit)
    }
}

case class Storage(playerData: Map[PlayerId, PlayerData], schedule: Map[ScId, ScheduleEntry]) {
  def players =
    playerData.toList.map(_._2)

  def putPlayerData(id: PlayerId, data: PlayerData): Storage =
    copy(playerData = playerData + (id -> data))

  def updatePlayerData(id: PlayerId, f: PlayerData => PlayerData): Storage =
    playerData.get(id).fold(this){data => copy(playerData = playerData.updated(id, f(data)))}

  def removePlayer(id: PlayerId) = {
    copy(playerData = playerData - id)
  }

  def addAchievements(achievements: List[EarnedAchievemnt]): Storage = {
    achievements match {
      case Nil          => this
      case list@head::tail => updatePlayerData(head.player.player.id, _.addAchievement(head.achievemnt)).addAchievements(tail)
    }
  }

  def playersPresentAt(area: Area) = {
    players
      .filter(_.lastLocation.area === area)
  }

  def addEntry(scheduleEntry: ScheduleEntry) =
    copy(schedule = schedule + (scheduleEntry.id -> scheduleEntry))

  def removeEntry(scId: ScId) =
    copy(schedule = schedule - scId)

  def updateEntry(scId: ScId, f: ScheduleEntry => ScheduleEntry) = {
    if (schedule.isDefinedAt(scId))
      copy(schedule = schedule + (scId -> f(schedule(scId))))
    else
      this
  }

  def entriesList =
    schedule.toList.map(_._2).sorted(ScheduleEntry.scheduleEntryOrder.toScalaOrdering)
}

case class PlayerData(
  player: Player,
  achievements: Set[Achievement],
  lastLocation: LocationUpdate,
  progress: PatternTracker[Achievement],
  platform: NotificationTarget) {

  def addAchievement(achievemnt: Achievement): PlayerData =
    copy(achievements = achievements + achievemnt)


  def addMovement(location: LocationUpdate): PlayerData =
    copy(lastLocation = location)

  override def toString =
    "Player(" + player.nick.value + ", " + platform + ")"
}

object PlayerData {

  implicit val playerDataEqual: Equal[PlayerData] =
    Equal.equalA[String].contramap((pd: PlayerData) => pd.player.id.value)

  def updateProgess: PatternTracker[Achievement] => PlayerData => PlayerData =
    progress => data => data.copy(progress = progress)

}
