package techex.cases

import org.joda.time.DateTime
import techex.data.{StreamEvent, Schedule}
import techex.domain._

import scalaz.State
import scalaz._, Scalaz._
import scalaz.stream.{process1, Process1}

object updateSchedule {

  type SchedS = State[Schedule, List[ScheduleEvent]]

  def handleSchedulingProcess1: Process1[StreamEvent, State[Schedule, List[ScheduleEvent]]] = {
    process1.lift(handleScheduling)
  }

  def handleScheduling: StreamEvent => State[Schedule, List[ScheduleEvent]] = {
    case AddEntry(entry)      => addEntry(entry)
    case RemoveEntry(entryId) => removeEntry(entryId)
    case StartEntry(entryId)  => startEntry(entryId)
    case EndEntry(entryId)    => endEntry(entryId)
    case _                    => State.state(nil)
  }


  def addEntry(entry: ScheduleEntry): SchedS =
    State { sch =>
      (sch.addEntry(entry), List(Added(entry)))
    }

  def removeEntry(entryId: ScId): SchedS =
    State { sch =>
      val maybeEntry =
        sch.entries.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.removeEntry(entryId), List(Removed(entry)))
      }
    }

  def startEntry(entryId: ScId): SchedS = {
    State { sch =>

      val maybeEntry =
        sch.entries.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.updateEntry(entryId, _.start), List(Started(DateTime.now, entry)))
      }

    }
  }

  def endEntry(entryId: ScId): SchedS = {
    State { sch =>

      val maybeEntry =
        sch.entries.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.updateEntry(entryId, _.stop), List(Ended(DateTime.now, entry)))
      }

    }
  }

}
