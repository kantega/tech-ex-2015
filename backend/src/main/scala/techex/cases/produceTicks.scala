package techex.cases

import java.util.concurrent.{TimeUnit, ScheduledExecutorService}

import org.joda.time._
import techex._
import techex.domain.{EndOfDay, StartOfDay, Ticks}

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream._
import Process._

object produceTicks {

  val timer =
    namedSingleThreadScheduler("Timer")

  def toMidnight(time: DateTime) =
    time.withTime(0, 0, 0, 0).plusDays(1)

  val days: Process[Task, Ticks] =
    //awakeEvery(DateTime.now().toInstant, Seconds.ONE, i => StartOfDay(i),i=>EndOfDay(i))
    awakeEvery(toMidnight(DateTime.now()).toInstant, Days.ONE, i => StartOfDay(i),i=>EndOfDay(i))

  def awakeEvery(start: Instant, d: ReadablePeriod, begin: Instant => Ticks, end: Instant => Ticks)(
    implicit S: Strategy): Process[Task, Ticks] = {
    def metronomeAndSignal: (() => Unit, async.mutable.Signal[Ticks]) = {
      val signal = async.signal[Ticks](S)

      val metronome = timer.scheduleAtFixedRate(
        new Runnable {
          def run = {
            signal.set(end(Instant.now())).run
            signal.set(begin(Instant.now())).run
          }
        },
        start.getMillis - Instant.now().getMillis,
        d.getMillis,
        TimeUnit.MILLISECONDS
      )
      (() => metronome.cancel(false), signal)
    }

    Process.await(Task.delay(metronomeAndSignal))({
      case (cm, signal) => signal.discrete onComplete eval_(signal.close.map(_ => cm()))
    })
  }
}
