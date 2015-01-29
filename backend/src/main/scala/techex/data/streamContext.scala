package techex.data

import techex.domain._

import scalaz.concurrent.Task

object streamContext {
  val historySize = 100

  private var playerContext:PlayerContext =
    PlayerContext(Map())

  def getPlayerContext:Task[PlayerContext] = {
    Task(playerContext)
  }

  def updatePlayerContext:PlayerContext => Task[PlayerContext] =
  newContext => Task{
    playerContext = newContext
    newContext
  }

}

case class PlayerContext(playerData: Map[PlayerId, PlayerData]) {
  def players =
  playerData.toList.map(_._2)

  def putPlayerData(id: PlayerId, data: PlayerData): PlayerContext =
    copy(playerData = playerData + (id -> data))

  def updatePlayerData(id: PlayerId, f: PlayerData => PlayerData) =
    copy(playerData = playerData.updated(id, f(playerData(id))))

  def addActivities(activities: List[Activity]): PlayerContext =
    activities match {
      case Nil          => this
      case head :: tail => updatePlayerData(head.playerId, _.addActivity(head)).addActivities(tail)
    }
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
