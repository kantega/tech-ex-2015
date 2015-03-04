package techex.domain

import techex.domain.patternmatching.Pred

import scalaz._, Scalaz._



object progresstracker {
  implicit def matcherMonoid[A]: Monoid[PatternTracker[A]] =
    Monoid.instance({ case (m1: PatternTracker[A], m2: PatternTracker[A]) => AndTracker(m1, m2)}, ZeroTracker[A]())

  def zero[A]: PatternTracker[A] =
    ZeroTracker[A]()

  def value[A,F](m: Matcher[F], value: A): PatternTracker[A] =
    ValueOnMatchTracker(m)(value)

  def collect[C, F, A](m: Matcher[F], f: F => Option[C])(g: Int => Option[A]): PatternTracker[A] =
    StatefulTracker[Set[C], F, A](m, Set()) { token => State { set =>
      val value = f(token)

      val newSet =
        value.fold(set)(v=> set + v)

      val isIncrease =
        set.size != newSet.size

      val badge =
        if (isIncrease)
          g(newSet.size)
        else none

      (newSet, badge)
    }
    }
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

  def zeroOnHalt[F](m: Matcher[F], p: PatternTracker[A]): PatternTracker[A] = {
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

case class OnMatchTracker[A](m: Matcher[Nothing])(f: Pattern => A) extends PatternTracker[A] {
  def apply(t: Fact) = {
    val (next, pattern) =
      m.check(t)
    val nextTracker = zeroOnHalt(next, OnMatchTracker(next)(f))

    (nextTracker, pattern.map(f).toList)
  }
}

case class ValueOnMatchTracker[A,F](m: Matcher[F])(f: => A) extends PatternTracker[A] {
  def apply(t: Fact) = {
    val (next, pattern) =
      m.check(t)
    val nextTracker = zeroOnHalt(next, ValueOnMatchTracker(m)(f))
    (nextTracker, pattern.map(x => f).toList)
  }
}
case class StatefulTracker[S, P, A](m: Matcher[P], s: S)(f: P => State[S, Option[A]]) extends PatternTracker[A] {

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

case class PredStatefulTracker[S, A](pred: Pred[Fact], s: S)(f: Fact => State[S, List[A]]) extends PatternTracker[A] {


  def apply(t: Fact): (PatternTracker[A], List[A]) = {

    val changes: State[S, List[A]] =
      if (pred(t))
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


