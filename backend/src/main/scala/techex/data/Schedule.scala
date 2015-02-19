package techex.data

import java.util.concurrent.Executors

import techex.domain.{ScId, ScheduleEntry}

import scalaz._
import scalaz.concurrent.Task
import scalaz.stream._

object ScheduleStore {

  implicit val executor =
    Executors.newSingleThreadExecutor()

  private var schedule =
    Schedule(Map())

  def run[A](state: State[Schedule, A]): Task[A] = {
    Task {
      val (ctx, a) = state(schedule)
      schedule = ctx
      a
    }(executor)
  }

  def updates[A]: Channel[Task, State[Schedule, A], A] =
    Process.constant(run)

}
case class Schedule(entries: Map[ScId, ScheduleEntry]) {

  def addEntry(scheduleEntry: ScheduleEntry) =
    copy(entries = entries + (scheduleEntry.id -> scheduleEntry))

  def removeEntry(scId: ScId) =
    copy(entries = entries - scId)

  def updateEntry(scId: ScId, f: ScheduleEntry => ScheduleEntry) = {
    if (entries.isDefinedAt(scId))
      copy(entries = entries + (scId -> f(entries(scId))))
    else
      this
  }

  def entriesList =
    entries.toList.map(_._2).sorted(ScheduleEntry.scheduleEntryOrder.toScalaOrdering)

}
