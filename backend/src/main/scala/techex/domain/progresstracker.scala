package techex.domain

import techex.domain.patternmatching.Pred

import scalaz._, Scalaz._



object progresstracker {
  implicit def matcherMonoid[A]: Monoid[PatternTracker[A]] =
    Monoid.instance({ case (m1: PatternTracker[A], m2: PatternTracker[A]) => AndTracker(m1, m2)}, ZeroTracker[A]())

  def zero[A]: PatternTracker[A] =
    ZeroTracker[A]()

  def value[A](value: A,m: Matcher): PatternTracker[A] =
    ValueOnMatchTracker(m)(value)
}


trait PatternTracker[A] {
  def apply(t: Fact): (PatternTracker[A], List[A])

  def and(other: PatternTracker[A]): PatternTracker[A] =
    (this, other) match {
      case (ZeroTracker(), other) => other
      case (first, ZeroTracker()) => first
      case (first, second)        => AndTracker(first, other)
    }

  def ++(other: PatternTracker[A]) =
    (this, other) match {
      case (ZeroTracker(), other) => other
      case (one, ZeroTracker())   => one
      case (one, other)           => AndTracker(one, other)
    }

  def zeroOnHalt(m: Matcher, p: PatternTracker[A]): PatternTracker[A] = {
    m match {
      case Halt() => ZeroTracker[A]()
      case _      => p
    }
  }

}

case class ZeroTracker[A]() extends PatternTracker[A] {
  def apply(t: Fact) = (this, Nil)
}


case class AndTracker[A](one: PatternTracker[A], other: PatternTracker[A]) extends PatternTracker[A] {
  def apply(t: Fact) = {
    val (next1, tokens1) = one(t)
    val (next2, tokens2) = other(t)

    (next1 ++ next2, tokens1 ::: tokens2)
  }
}

case class OnMatchTracker[A](m: Matcher)(f: Pattern => A) extends PatternTracker[A] {
  def apply(t: Fact) = {
    val (next, pattern) =
      m.check(t)
    val nextTracker = zeroOnHalt(next, OnMatchTracker(next)(f))

    (nextTracker, pattern.map(f).toList)
  }
}

case class ValueOnMatchTracker[A](m: Matcher)(f: => A) extends PatternTracker[A] {
  def apply(t: Fact) = {
    val (next, pattern) =
      m.check(t)
    val nextTracker = zeroOnHalt(next, ValueOnMatchTracker(m)(f))
    (nextTracker, pattern.map(x => f).toList)
  }
}
case class StatefulTracker[S, A](m: Matcher, s: S)(f: Pattern => State[S, Option[A]]) extends PatternTracker[A] {

  type StateS[x] = State[S, x]

  def apply(t: Fact): (PatternTracker[A], List[A]) = {
    val (next, matches) =
      m.check(t)

    val (ns, out) = matches.fold((s, none[A])) { pattern =>
      f(pattern).run(s)
    }


    (StatefulTracker(next, ns)(f), out.toList)
  }
}

case class PredStatefulTracker[S, A](pred: Pred, s: S)(f: Fact => State[S, List[A]]) extends PatternTracker[A] {


  def apply(t: Fact): (PatternTracker[A], List[A]) = {

    val changes: State[S, List[A]] =
      if (pred(Pattern(List(t))))
        f(t)
      else
        State.state(nil)

    val output =
      changes.run(s)

    (PredStatefulTracker(pred, output._1)(f), output._2)
  }
}

case class CasedStatefulTracker[S, A](s: S)(f: PartialFunction[Fact, State[S, List[A]]]) extends PatternTracker[A] {

  def apply(t: Fact): (PatternTracker[A], List[A]) = {

    val state: State[S, List[A]] =
      if (f.isDefinedAt(t))
        f(t)
      else
        State.state(nil)

    val output =
      state.run(s)

    (CasedStatefulTracker(output._1)(f), output._2)
  }
}


