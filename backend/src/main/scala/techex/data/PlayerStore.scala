package techex.data

import java.util.concurrent.Executors

import techex.domain._
import scalaz._, Scalaz._
import scalaz.State
import scalaz.concurrent.Task
import scalaz.stream.{Process, Channel}

object PlayerStore {


  val historySize = 100

  implicit val executor =
    Executors.newSingleThreadExecutor()

  private var playerContext: PlayerStore =
    PlayerStore(Map())


  def update(f: PlayerStore => PlayerStore): State[PlayerStore, PlayerStore] = State(
    ctx => {
      val newCtx = f(ctx)
      (newCtx, newCtx)
    }
  )

  def read[A](f: PlayerStore => A): State[PlayerStore, A] = State(
    ctx => {
      val a = f(ctx)
      (ctx, a)
    }
  )

  def run[A](state: State[PlayerStore, A]): Task[A] = {
    Task {
      val (ctx, a) = state(playerContext)
      playerContext = ctx
      a
    }(executor)
  }

  def updates[A]: Channel[Task, State[PlayerStore, A], A] =
    Process.constant(run)

  def playersPresentAt(area: Area): State[PlayerStore, List[PlayerData]] = {
    State.gets { ctx =>
      ctx.playersPresentAt(area)
    }
  }

  def updatePlayer: PlayerId => (PlayerData => PlayerData) => State[PlayerStore, Unit] =
    id => f => State { ctx =>
      (ctx.updatePlayerData(id, f), Unit)
    }
}

case class PlayerStore(playerData: Map[PlayerId, PlayerData]) {
  def players =
    playerData.toList.map(_._2)

  def putPlayerData(id: PlayerId, data: PlayerData): PlayerStore =
    copy(playerData = playerData + (id -> data))

  def updatePlayerData(id: PlayerId, f: PlayerData => PlayerData): PlayerStore =
    copy(playerData = playerData.updated(id, f(playerData(id))))

  def removePlayer(id: PlayerId) = {
    copy(playerData = playerData - id)
  }

  def addFacts(activities: List[FactUpdate]): PlayerStore =
    activities match {
      case Nil          => this
      case head :: tail => updatePlayerData(head.info.playerId, _.addFact(head)).addFacts(tail)
    }

  def playersPresentAt(area: Area) = {
    players
      .filter(_.movements.headOption.exists(lu => lu.area === area))
  }
}

case class PlayerData(
  player: Player,
  achievements: Set[Badge],
  movements: Vector[LocationUpdate],
  activities: Vector[FactUpdate],
  progress: PatternOutput[Badge],
  platform:NotificationTarget) {

  def addAchievement(achievemnt: Badge): PlayerData =
    copy(achievements = achievements + achievemnt)

  def addMovement(location: LocationUpdate): PlayerData =
    copy(movements = {
      val updated =
        location +: movements

      if (updated.size > PlayerStore.historySize)
        updated.init
      else
        updated
    })

  def addActivities(activities: List[FactUpdate]): PlayerData =
    activities.foldRight(this) { (activity, data) => data.addFact(activity)}

  def addFact(activity: FactUpdate): PlayerData =
    copy(activities = activity +: activities)

}

object PlayerData {

  implicit val playerDataEqual: Equal[PlayerData] =
    Equal.equalA[String].contramap((pd: PlayerData) => pd.player.id.value)

  def updateProgess: PatternOutput[Badge] => PlayerData => PlayerData =
    progress => data => data.copy(progress = progress)

}
