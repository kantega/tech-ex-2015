package techex.domain

import scalaz._, Scalaz._

import preds._
import matching._

object matching {

  def await(f: Match => PatternMatcher) = {
    Await(GenAwaitFun(f))
  }
}


object preds {
  type Pred = Match => Boolean

  def exists(pred: Pred) =
    Await(Exists(pred))

  case class PredOps(pred: Pred) {
    def and(other: Pred) =
      preds.and(pred, other)

    def or(other: Pred) =
      preds.or(pred, other)

  }

  implicit def toInfixOps(pred: Pred): PredOps =
    PredOps(pred)

  implicit def toExists(pred: Pred): PatternMatcher =
    Await(Exists(pred))

  def not(pred: Pred): Pred =
    ctx => !pred(ctx)

  def and(one: Pred, other: Pred): Pred =
    ctx => one(ctx) && other(ctx)

  def or(one: Pred, other: Pred): Pred =
    ctx => one(ctx) || other(ctx)


  def visited(area: Area) =
    fact({ case ArrivedAtArea(_, a, _) if a === area => true})


  def ctx(f: PartialFunction[(List[Fact]), Boolean]): Pred =
    ctx => {
      if (f.isDefinedAt(ctx.pattern))
        f(ctx.pattern)
      else
        false
    }


  def fact(f: PartialFunction[Fact, Boolean]): Pred =
    ctx => {
      if (f.isDefinedAt(ctx.pattern.head))
        f(ctx.pattern.head)
      else
        false
    }


  def matched(f: PartialFunction[Fact, Boolean]): Fact => Boolean = {
    update =>
      if (f.isDefinedAt(update))
        f(update)
      else
        false
  }
}

sealed trait PatternMatcher {

  def parse(fact: Fact): (PatternMatcher, List[Match]) = {
    val nxt = apply(Match(List(fact)))

    nxt match {
      case Halt()                  => (Halt(), nil)
      case Continue(matches, next) => (next, matches)
      case Await(f)                => (Await(f), nil)
    }
  }

  def apply(fact: Match): PatternMatcher = {
    def go(p: PatternMatcher): PatternMatcher = {
      p match {
        case Halt()                  => Halt()
        case Continue(matches, next) => next(fact) match {
          case Continue(m2, n2) => go(Continue(m2 ::: matches, n2))
          case _                => Continue(matches, next)
        }
        case Await(f)                => f(fact)
      }
    }

    go(this)
  }

  def append(matched: Match): PatternMatcher = {
    this match {
      case Halt()               => Halt()
      case Continue(emit, next) => Continue(matched.appendToAll(emit), next)
      case Await(f)             => await(m => f(m :: matched))
    }
  }

  def ++(p: PatternMatcher): PatternMatcher =
    (this, p) match {
      case (Halt(), Halt())                                     => Halt()
      case (Halt(), other)                                      => other
      case (one, Halt())                                        => one
      case (Continue(tokens1, next1), Continue(tokens2, next2)) => Continue(tokens1 ::: tokens2, next1 ++ next2)
      case (m@Continue(tokens, next), waiting: Await)           => Continue(tokens, next ++ waiting)
      case (waiting: Await, m@Continue(tokens, next))           => Continue(tokens, next ++ waiting)
      case (first: Await, second: Await)                        => Await(AndWaiting(first, second))
    }

  def ~>(p: PatternMatcher): PatternMatcher =
    Await(FBy(this, p, Nil))

  def ~><(p: PatternMatcher): PatternMatcher =
    Await(FBy(this, p, Nil))

  def never: PatternMatcher = {
    this match {
      case Halt()      => Halt()
      case c: Continue => Halt()
      case Await(f)    => await(m => f(m).never)
    }
  }

  def once: PatternMatcher = {
    this match {
      case Halt()               => Halt()
      case Continue(Nil, next)  => Continue(Nil, next.once)
      case Continue(emit, next) => Continue(emit, Halt())
      case Await(f)             => await(m => f(m).once)
    }
  }

}

case class Match(pattern: List[Fact]) {
  def ::(m: Match) = Match(m.pattern ::: pattern)

  def appendToAll(list: List[Match]) =
    list.map(m => Match(m.pattern ::: pattern))

}
case class Await(f: AwaitFun) extends PatternMatcher
case class Continue(matches: List[Match], next: PatternMatcher) extends PatternMatcher
case class Halt() extends PatternMatcher

trait AwaitFun{
  def apply(t:Match):PatternMatcher
}

case class AndWaiting(one: Await, other: Await) extends AwaitFun {
  def apply(t: Match) =
    one.f(t) ++ other.f(t)

  override def toString =
    " ( " + one.toString + " AND " + other.toString + " ) "
}


case class FBy(
  one: PatternMatcher,
  other: PatternMatcher,
  window: List[PatternMatcher]) extends AwaitFun {

  def apply(fact: Match): PatternMatcher = {

    val (output, keepInWindow) =
      window.map(a => a(fact)).foldLeft((nil[Match], nil[PatternMatcher])) { (lists, pm) =>
        pm match {
          case Halt()                 => lists
          case Continue(Nil, next) => (lists._1, next :: lists._2)
          case Continue(tokens, _) => (tokens ::: lists._1, lists._2)
          case a: Await               => (lists._1, a :: lists._2)
        }
      }

    one(fact) match {
      case Halt()                                     => Halt()
      case Continue(Nil, next) if output.nonEmpty     =>
        Continue(output, Await(FBy(next, other, keepInWindow)))
      case Continue(Nil, next) if output.isEmpty      =>
        Await(FBy(one, other, output.map(other.append) ::: keepInWindow))
      case Continue(matches, next) if output.nonEmpty =>
        Continue(output, Await(FBy(one, other, matches.map(other.append) ::: keepInWindow)))
      case Continue(matches, next) if output.isEmpty  =>
        Await(FBy(one, other, matches.map(other.append) ::: keepInWindow))
      case a: Await if output.nonEmpty                =>
        Continue(output, Await(FBy(one, other, Await(FBy(a, other, Nil)) :: keepInWindow)))
      case a: Await if output.isEmpty                 =>
        Await(FBy(one, other, Await(FBy(a, other, Nil)) :: keepInWindow))
    }
  }
}
case class GenAwaitFun(f: Match => PatternMatcher) extends AwaitFun {
  def apply(m: Match) = f(m)
}

case class Exists(pred: Pred) extends AwaitFun {
  def apply(t: Match) = {
    if (pred(t))
      Continue(List(t), Await(this))
    else
      Continue(nil, Await(this))
  }

  override def toString() = "[?]"
}


trait PatternTracker[A] {
  def apply(t: Fact): (PatternTracker[A], List[A])

  def and(other: PatternTracker[A]): PatternTracker[A] =
    (this, other) match {
      case (ZeroTracker(), other) => other
      case (first, ZeroTracker()) => first
      case (first, second)        => AndTracker(first, other)
    }

}

object PatternTracker {
  implicit def matcherMonoid[A]: Monoid[PatternTracker[A]] =
    Monoid.instance({ case (m1: PatternTracker[A], m2: PatternTracker[A]) => AndTracker(m1, m2)}, ZeroTracker[A]())

  def zero[A]: PatternTracker[A] =
    ZeroTracker[A]()
}

case class ZeroTracker[A]() extends PatternTracker[A] {
  def apply(t: Fact) = (this, Nil)
}


case class AndTracker[A](one: PatternTracker[A], other: PatternTracker[A]) extends PatternTracker[A] {
  def apply(t: Fact) = {
    val (next1, tokens1) = one(t)
    val (next2, tokens2) = other(t)

    (AndTracker(next1, next2), tokens1 ::: tokens2)
  }
}

case class CountingTracker[A](current: Int, max: Int, pattern: PatternMatcher)(f: Int => Option[A]) extends PatternTracker[A] {
  def apply(t: Fact): (PatternTracker[A], List[A]) = {
    val (next, tokens) =
      pattern.parse(t)

    val output =
      tokens.zipWithIndex
        .map { case (token, n) => if (n + current <= max) f(n + current) else None}
        .collect { case Some(x) => x}

    if (current == max)
      (ZeroTracker(), output)
    else {
      next match {
        case Halt() => (ZeroTracker(), output)
        case _      => (CountingTracker(current + tokens.length, max, next)(f), output)
      }

    }

  }
}

case class StatefulTracker[S, A](pattern: PatternMatcher, s: S)(f: Match => State[S, Option[A]]) extends PatternTracker[A] {

  type StateS[x] = State[S, x]

  def apply(t: Fact): (PatternTracker[A], List[A]) = {
    val (next, matches) =
      pattern.parse(t)

    val changes =
      matches
        .map(f)
        .sequence[StateS, Option[A]]

    val output =
      changes.run(s)

    (StatefulTracker(next, output._1)(f), output._2.collect { case Some(x) => x})
  }
}

case class PredStatefulTracker[S, A](pred: Pred, s: S)(f: Fact => State[S, List[A]]) extends PatternTracker[A] {


  def apply(t: Fact): (PatternTracker[A], List[A]) = {

    val changes:State[S, List[A]] =
      if(pred(Match(List(t))))
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

    val state:State[S, List[A]] =
      if(f.isDefinedAt(t))
        f(t)
    else
        State.state(nil)

    val output =
      state.run(s)

    (CasedStatefulTracker(output._1)(f), output._2)
  }
}


