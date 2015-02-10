package techex

import scalaz.stream.{process1, Process1}

object streams {
  def mapAccum[S,I,O](s: S)(g: (S, I) => (S, O)): Process1[I,O] =
    process1.zipWithScan1[I, (S, Option[O])]((s, None)) { case (a, (sn, _)) =>
      val (s2, o) = g(sn, a)
      (s2, Some(o))
    }.map(_._2._2).pipe(process1.stripNone)
}
