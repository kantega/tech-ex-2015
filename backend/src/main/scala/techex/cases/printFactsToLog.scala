package techex.cases

import doobie.util.process
import techex.domain._
import techex.streams

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task
import scalaz.stream.{Process, Sink}

object printFactsToLog {

  val notificationQueue =
    scalaz.stream.async.unboundedQueue[Fact]

  def setup(facts: Process[Task, Fact]): Task[Unit] =
    Task {
      (facts to notificationQueue.enqueue).run.runAsync(println(_))
      (notificationQueue.dequeue to sender)
        .handle(streams.printAndReset(notificationQueue.dequeue to sender))
        .run.runAsync(println(_))
    }


  def sender: Sink[Task, Fact] =
    process.sink(printFact)


  def printFact: Fact => Task[Unit] = {
    fact => Task {
      println(fact.toString)
    }
  }

}
