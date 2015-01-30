package techex.data

import java.util.concurrent.Executors

import techex.domain._

import scalaz.State
import scalaz.concurrent.Task

object streamContext {


  val historySize = 100

  implicit val executor =
    Executors.newSingleThreadExecutor()

  private var playerContext: PlayerContext =
    PlayerContext(Map())


  def update(f: PlayerContext => PlayerContext): State[PlayerContext, PlayerContext] = State(
    ctx => {
      val newCtx = f(ctx)
      (newCtx, newCtx)
    }
  )

  def read[A](f: PlayerContext => A): State[PlayerContext, A] = State(
    ctx => {
      val a = f(ctx)
      (ctx, a)
    }
  )

  def run[A](state: State[PlayerContext, A]): Task[A] = {
    Task {
      val (ctx, a) = state(playerContext)
      playerContext = ctx
      a
    }
  }




}

case class PlayerContext(playerData: Map[PlayerId, PlayerData]) {
  def players =
    playerData.toList.map(_._2)

  def putPlayerData(id: PlayerId, data: PlayerData): PlayerContext =
    copy(playerData = playerData + (id -> data))

  def updatePlayerData(id: PlayerId, f: PlayerData => PlayerData): PlayerContext =
    copy(playerData = playerData.updated(id, f(playerData(id))))

  def addActivities(activities: List[Activity]): PlayerContext =
    activities match {
      case Nil          => this
      case head :: tail => updatePlayerData(head.playerId, _.addActivity(head)).addActivities(tail)
    }
}

object PlayerContext {




}

case class PlayerData(player: Player, achievements: Set[Badge], movements: Vector[LocationUpdate], activities: Vector[Activity]) {

  def addAchievement(achievemnt: Badge): PlayerData =
    copy(achievements = achievements + achievemnt)

  def addMovement(location: LocationUpdate): PlayerData =
    copy(movements = {
      val updated =
        location +: movements

      if (updated.size > streamContext.historySize)
        updated.init
      else
        updated
    })

  def addActivities(activities: List[Activity]): PlayerData =
    activities.foldRight(this) { (activity, data) => data.addActivity(activity)}

  def addActivity(activity: Activity): PlayerData =
    copy(activities = activity +: activities)

}
