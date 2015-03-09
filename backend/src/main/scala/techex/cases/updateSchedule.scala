package techex.cases

import org.joda.time.{Instant, DateTime}
import techex.data._
import techex.domain._

import scalaz.State

object updateSchedule {

  type SchedS = State[Storage, List[Fact]]

  def handleScheduling: PartialFunction[InputMessage, State[Storage, List[Fact]]] = {
    case AddEntry(entry, instant)      => addEntry(entry, instant)
    case RemoveEntry(entryId, instant) => removeEntry(entryId, instant)
    case StartEntry(entryId, instant)  => startEntry(entryId, instant)
    case EndEntry(entryId, instant)    => endEntry(entryId, instant)
  }


  def addEntry(entry: ScheduleEntry, instant: Instant): SchedS =
    State { sch =>
      (sch.addEntry(entry), List(Added(entry, instant)))
    }

  def removeEntry(entryId: ScId, instant: Instant): SchedS =
    State { sch =>
      val maybeEntry =
        sch.schedule.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.removeEntry(entryId), List(Removed(entry, instant)))
      }
    }

  def startEntry(entryId: ScId, instant: Instant): SchedS = {
    State { sch =>

      val maybeEntry =
        sch.schedule.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.updateEntry(entryId, _.start), List(Started(entry.start, instant)))
      }

    }
  }

  def endEntry(entryId: ScId, instant: Instant): SchedS = {
    State { sch =>

      val maybeEntry =
        sch.schedule.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.updateEntry(entryId, _.stop), List(Ended(entry, instant)))
      }

    }
  }

}
