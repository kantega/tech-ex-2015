package techex

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.stream.{process1, Process1,Process}

object streams {
  def mapAccum[S, I, O](s: S)(g: (S, I) => (S, O)): Process1[I, O] =
    process1.zipWithScan1[I, (S, Option[O])]((s, None)) { case (a, (sn, _)) =>
      val (s2, o) = g(sn, a)
      (s2, Some(o))
    }.map(_._2._2).pipe(process1.stripNone)

  def appendAccumP1[A,S](g: A => State[S, List[A]]):Process1[State[S, List[A]],State[S, List[A]]] =
  process1.lift(appendAccum(g))

  def appendAccum[A, S](g: A => State[S, List[A]]): State[S, List[A]] => State[S, List[A]] =
    s0 => s0.flatMap { list =>
      State { s1 =>
        list.foldLeft((s1, list)) { (thread, an) =>
          val tempState =
            thread._1

          val accumList =
            thread._2

          val (nextState, listToadd) =
            g(an)(tempState)

          (nextState, accumList ::: listToadd )
        }
      }
    }

  def printAndReset[A](p:Process[Task,A]):PartialFunction[Throwable,Process[Task,A]] = {
    case t:Throwable =>
      t.printStackTrace()
      p
  }

}
