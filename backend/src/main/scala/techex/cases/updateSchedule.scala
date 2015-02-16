package techex.cases

import techex.data.{StreamEvent, Schedule}

import scalaz.State
import scalaz.stream.{process1, Process1}

object updateSchedule {

  def handleScheduling:Process1[StreamEvent,State[Schedule,List[StreamEvent]]] = {
    process1.lift(event =>
      null
    )
  }


}
