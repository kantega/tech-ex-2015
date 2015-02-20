package techex.domain


import org.joda.time.Hours

import scalaz._, Scalaz._
import matching._

object matching {


  implicit val defaultExpireDuration =
    Hours.THREE.toStandardDuration

  def exists(pred: Pred): EventPattern =
    Waiting(AwaitOccurence(pred))

  def notExists(pred: Pred): EventPattern = {
    Matched(Nil, Waiting(HaltOnOccurence(pred)))
  }

}

case class Pred(p: Token => Boolean, desc: String) {
  def apply(t: Token) = p(t)
}

object predicates {

  case class PredOps(pred: Pred) {
    def and(other: Pred) =
      predicates.and(pred, other)

    def or(other: Pred) =
      predicates.or(pred, other)

    def ~>(other: Pred) =
      exists(pred) ~> exists(other)

    def ~>(other: EventPattern) =
      exists(pred) ~> other

    def ~><(other: Pred) =
      exists(pred) ~>< exists(other)

    def ~><(other: EventPattern) =
      exists(pred) ~>< other

  }

  implicit def toInfixOps(pred: Pred): PredOps =
    PredOps(pred)

  def not(pred: Pred): Pred =
    Pred(ctx => !pred(ctx), "not " + pred.desc)

  def and(one: Pred, other: Pred): Pred =
    Pred(ctx => one(ctx) && other(ctx), one.desc + " and " + other.desc)

  def or(one: Pred, other: Pred): Pred =
    Pred(ctx => one(ctx) || other(ctx), one.desc + " or " + other.desc)


  def visited(area: Area) =
    fact({ case ArrivedAtArea(_, a) if a === area => true})


  def ctx(f: PartialFunction[(Fact, List[Fact]), Boolean]) =
    Pred(
      ctx => {
        if (f.isDefinedAt((ctx.fact, ctx.matches)))
          f((ctx.fact, ctx.matches))
        else
          false
      }, "Match pattern"
    )


  def fact(f: PartialFunction[Fact, Boolean]) =
    Pred(
      ctx => {
        if (f.isDefinedAt(ctx.fact))
          f(ctx.fact)
        else
          false
      }, "Match by fact"
    )


  def matched(f: PartialFunction[Fact, Boolean]): Fact => Boolean = {
    update =>
      if (f.isDefinedAt(update))
        f(update)
      else
        false
  }
}


case class Token(fact: Fact, matches: List[Fact]) {

  def add(t: Fact): Token = copy(matches = t :: matches)

  def addToken = add(fact)

  def appendMatchesFrom(other: Token) =
    Token(fact, matches ::: other.matches)

  def appendHistory(history: List[Fact]): Unit = {
    Token(fact, matches ::: history)
  }

  override def toString: String =
    "Token(fact:" + fact + " - matches: [" + matches.reverse.mkString(" ~> ") + "])"
}

object Token {

  def append(last: List[Token], prev: List[Token]): List[Token] = {
    for {
      t1 <- last
      t2 <- prev
    } yield t1.appendMatchesFrom(t2)
  }

}

sealed trait EventPattern {

  def parse(s: Token): (EventPattern, List[Token]) = {
    def go(p: EventPattern): (EventPattern, List[Token]) = {

      p match {
        case Halted()              => (Halted(), Nil)
        case Matched(tokens, next) => go(next).bimap(x => x, _ ::: tokens)
        case Waiting(f)            => f(s) match {
          case Halted()              => (Halted(), Nil)
          case Matched(tokens, next) => (next, tokens)
          case a@Waiting(f)          => (a, Nil)
        }
      }
    }
    //println("")
    //println(">>>  " + s.fact.fact)
    val result = go(this)
    //println("    " + result._1)
    //println("<<< " + result._2)

    result
  }

  def ++(p: EventPattern): EventPattern =
    (this, p) match {
      case (Halted(), Halted())                               => Halted()
      case (Halted(), other)                                  => other
      case (one, Halted())                                    => one
      case (Matched(tokens1, next1), Matched(tokens2, next2)) => Matched(tokens1 ::: tokens2, next1 ++ next2)
      case (m@Matched(tokens, next), waiting: Waiting)        => Matched(tokens, next ++ waiting) //Waiting(WaitingAndEmit(waiting,m))//
      case (waiting: Waiting, m@Matched(tokens, next))        => Matched(tokens, next ++ waiting) //Waiting(WaitingAndEmit(waiting,m))//
      case (first: Waiting, second: Waiting)                  => Waiting(AndWaiting(first, second))
    }

  def ~>(p: EventPattern): EventPattern = {
    (this, p) match {
      case (Halted(), _)                                      => Halted()
      case (_, Halted())                                      => Halted()
      case (Matched(tokens1, next1), Matched(tokens2, next2)) => Matched(tokens2, Halted())
      case (first: Matched, second: Waiting)                  => Waiting(AwaitSecond(first, second))
      case (first: Waiting, second: Matched)                  => Waiting(AwaitFirst(first, second))
      case (first: Waiting, second: Waiting)                  => Waiting(AwaitFirst(first, second))
    }
  }

  def ~>(pred: Pred): EventPattern = {
    this ~> exists(pred)
  }

  def ~><(until: EventPattern): EventPattern = {
    (this, until) match {
      case (Halted(), _)                      => Halted()
      case (_, Halted())                      => Halted()
      case (Waiting(pf), Matched(ut, un))     => Halted()
      case (Matched(pt, pn), Matched(ut, un)) => Matched(Token.append(ut, pt), pn ~>< un)
      case (Matched(pt, pn), Waiting(uf))     => pn.accum(pt) ~>< Waiting(uf)
      case (pw: Waiting, uw: Waiting)         => Waiting(Until(pw, uw))

    }
  }

  def ~><(pred: Pred): EventPattern = {
    this ~>< exists(pred)
  }

  def accum(history: List[Token]): EventPattern = {
    this match {
      case Halted()        => Halted()
      case Matched(tokens, next) => Matched(Token.append(tokens, history), next)
      case w@Waiting(f)          => Matched(history, Waiting(Accumulate(w,history)))
    }
  }
}

case class Matched(tokens: List[Token], next: EventPattern) extends EventPattern {
  override def toString() =
    "[" + tokens.map(x => x.fact).mkString(",") + "]"
}
case class Waiting(f: WaitFunc) extends EventPattern {
  override def toString() =
    "" + f + ""
}
case class Halted() extends EventPattern
trait WaitFunc {
  def apply(t: Token): EventPattern
}


case class Accumulate(p:Waiting,history:List[Token]) extends WaitFunc{
  def apply(t:Token) =
    p.f(t).accum(history)

}
case class AwaitSecond(first: Matched, second: Waiting) extends WaitFunc {
  def apply(t: Token) = {
    val next =
      first.tokens
        .map(prevMatch => second.f(t.appendMatchesFrom(prevMatch)))
        .foldLeft1Opt(_ ++ _).getOrElse(Halted())

    next match {
      case m: Matched => (first ~> next) ++ (first ~> second)
      case _          => first ~> second
    }
  }

  override def toString() =
    first + " ~> " + second
}

case class AwaitFirst(first: Waiting, second: EventPattern) extends WaitFunc {
  def apply(t: Token) = {
    val next =
      first.f(t)

    next match {
      case m: Matched => (next ~> second) ++ (first ~> second)
      case _          => first ~> second
    }
  }

  override def toString() =
    first + " ~> " + second
}

case class AwaitOccurence(pred: Pred) extends WaitFunc {
  def apply(t: Token) = {
    if (pred(t))
      Matched(List(t.addToken), Waiting(this))
    else
      Waiting(this)
  }

  override def toString() = "[?]"
}

case class HaltOnOccurence(pred: Pred) extends WaitFunc {
  def apply(t: Token) = {
    if (pred(t))
      Halted()
    else
      Matched(List(), Waiting(this))
  }

  override def toString() = "X"
}


case class Until(pattern: Waiting, until: Waiting) extends WaitFunc {
  def apply(t: Token) = {
    val wnext =
      until.f(t)

    val patternNext =
      pattern.f(t)
    patternNext ~>< wnext
  }

  override def toString =
    pattern + " ~>< " + until
}


case class AndWaiting(one: Waiting, other: Waiting) extends WaitFunc {
  def apply(t: Token) =
    one.f(t) ++ other.f(t)

  override def toString =
    " ( " + one.toString + " AND " + other.toString + " ) "
}

case class WaitingAndEmit(one: Waiting, other: Matched) extends WaitFunc {
  def apply(t: Token) =
    one.f(t) ++ other
}


trait PatternOutput[A] {
  def apply(t: FactAboutPlayer): (PatternOutput[A], List[A])

  def and(other: PatternOutput[A]): PatternOutput[A] =
    (this, other) match {
      case (ZeroMatcher(), other) => other
      case (first, ZeroMatcher()) => first
      case (first, second)        => AndMatcher(first, other)
    }

}

object PatternOutput {
  implicit def matcherMonoid[A]: Monoid[PatternOutput[A]] =
    Monoid.instance({ case (m1: PatternOutput[A], m2: PatternOutput[A]) => AndMatcher(m1, m2)}, ZeroMatcher[A]())

  def zero[A]: PatternOutput[A] =
    ZeroMatcher[A]()
}

case class ZeroMatcher[A]() extends PatternOutput[A] {
  def apply(t: FactAboutPlayer) = (this, Nil)
}


case class AndMatcher[A](one: PatternOutput[A], other: PatternOutput[A]) extends PatternOutput[A] {
  def apply(t: FactAboutPlayer) = {
    val (next1, tokens1) = one(t)
    val (next2, tokens2) = other(t)

    (AndMatcher(next1, next2), tokens1 ::: tokens2)
  }
}

case class CountingTracker[A](current: Int, max: Int, pattern: EventPattern)(f: Int => Option[A]) extends PatternOutput[A] {
  def apply(t: FactAboutPlayer): (PatternOutput[A], List[A]) = {
    val (next, tokens) =
      pattern.parse(Token(t, Nil))

    val output =
      tokens.zipWithIndex
        .map { case (token, n) => if (n + current <= max) f(n + current) else None}
        .collect { case Some(x) => x}

    if (current == max)
      (ZeroMatcher(), output)
    else {
      next match {
        case Halted() => (ZeroMatcher(), output)
        case _        => (CountingTracker(current + tokens.length, max, next)(f), output)
      }

    }

  }
}

case class StatefulTracker[S, A](pattern: EventPattern, s: S)(f: Token => State[S, Option[A]]) extends PatternOutput[A] {

  type StateS[x] = State[S, x]

  def apply(t: FactAboutPlayer): (PatternOutput[A], List[A]) = {
    val (next, matches) =
      pattern.parse(Token(t, Nil))

    val changes =
      matches
        .map(f)
        .sequence[StateS, Option[A]]

    val output =
      changes.run(s)

    (StatefulTracker(next, output._1)(f), output._2.collect { case Some(x) => x})
  }
}



