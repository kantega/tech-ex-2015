package techex

import scalaz._
import scalaz.\/._
import scalaz.stream._
import scalaz.concurrent._


object Streamtest extends App {

  def asyncReadInt(callback: Throwable \/ Int => Unit): Unit = {
    // imagine an asynchronous task which eventually produces an `Int`
    try {
      Thread.sleep(500)
      val result = (math.random * 100).toInt
      callback(right(result))
      Thread.sleep(500)
      val result2 = (math.random * 100).toInt
      callback(right(result2))
    } catch {case t: Throwable => callback(left(t))}
  }

  val intTask: Task[Int] = Task.async(asyncReadInt)

  val asyncInts: Process[Task, Int] = Process.eval(intTask)

  val sink: Sink[Task, Int] = Process.constant((int: Int) =>
    Task.now(println(int)))

  asyncInts.to(sink).run.runAsync(_.fold(t => t.printStackTrace(), n =>  println("OK")))

}
