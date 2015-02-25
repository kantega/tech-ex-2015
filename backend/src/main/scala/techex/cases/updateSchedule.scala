package techex.cases

import org.joda.time.DateTime
import techex.data._
import techex.domain._

import scalaz.State

object updateSchedule {

  type SchedS = State[Storage, List[Fact]]

  def handleScheduling: PartialFunction[InputMessage , State[Storage, List[Fact]]] = {
    case AddEntry(entry)      => addEntry(entry)
    case RemoveEntry(entryId) => removeEntry(entryId)
    case StartEntry(entryId)  => startEntry(entryId)
    case EndEntry(entryId)    => endEntry(entryId)
  }


  def addEntry(entry: ScheduleEntry): SchedS =
    State { sch =>
      (sch.addEntry(entry), List(Added(entry)))
    }

  def removeEntry(entryId: ScId): SchedS =
    State { sch =>
      val maybeEntry =
        sch.schedule.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.removeEntry(entryId), List(Removed(entry)))
      }
    }

  def startEntry(entryId: ScId): SchedS = {
    State { sch =>

      val maybeEntry =
        sch.schedule.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.updateEntry(entryId, _.start), List(Started(DateTime.now, entry.start)))
      }

    }
  }

  def endEntry(entryId: ScId): SchedS = {
    State { sch =>

      val maybeEntry =
        sch.schedule.get(entryId)

      maybeEntry match {
        case None        => (sch, Nil)
        case Some(entry) => (sch.updateEntry(entryId, _.stop), List(Ended(DateTime.now, entry)))
      }

    }
  }

}
