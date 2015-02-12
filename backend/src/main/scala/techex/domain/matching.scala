package techex.domain


import org.joda.time.{Hours, Instant, Duration}

import scalaz._, Scalaz._
import matching._

object matching {


  implicit val defaultExpireDuration =
    Hours.THREE.toStandardDuration

  def exists(pred: Pred): EventPattern =
    Waiting(AwaitOccurence(pred))

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

  }

  implicit def toInfixOps(pred: Pred): PredOps =
    PredOps(pred)

  def not(pred: Pred): Pred =
    Pred(ctx => !pred(ctx), "not " + pred.desc)

  def !!(pred: Pred): Pred =
    not(pred)

  def and(one: Pred, other: Pred): Pred =
    Pred(ctx => one(ctx) && other(ctx), one.desc + " and " + other.desc)

  def or(one: Pred, other: Pred): Pred =
    Pred(ctx => one(ctx) || other(ctx), one.desc + " or " + other.desc)


  def visited(area: Area) =
    fact({ case Entered(a) if a === area => true})


  def ctx(f: PartialFunction[(FactUpdate, List[FactUpdate]), Boolean]) =
    Pred(
      ctx => {
        if (f.isDefinedAt((ctx.fact, ctx.matches)))
          f((ctx.fact, ctx.matches))
        else
          false
      }, "Match pattern"
    )


  def update(f: PartialFunction[FactUpdate, Boolean]) =
    Pred(
      ctx => {
        if (f.isDefinedAt(ctx.fact))
          f(ctx.fact)
        else
          false
      }, "Match by fact"
    )

  def fact(f: PartialFunction[Fact, Boolean]) =
    Pred(
      ctx => {
        if (f.isDefinedAt(ctx.fact.fact))
          f(ctx.fact.fact)
        else
          false
      }, "Match by fact"
    )

  def matched(f: PartialFunction[Fact, Boolean]): FactUpdate => Boolean = {
    update =>
      if (f.isDefinedAt(update.fact))
        f(update.fact)
      else
        false
  }
}


case class Token(fact: FactUpdate, matches: List[FactUpdate]) {

  def add(t: FactUpdate): Token = copy(matches = t :: matches)

  def addToken = add(fact)

  def appendMatchesFrom(other: Token) =
    Token(fact, matches ::: other.matches)

  override def toString: String =
    "Token(fact:" + fact.fact + " - matches: [" + matches.reverse.map(_.fact).mkString(" ~> ") + "])"
}

object Token {

  def append(last: List[Token], prev: List[Token]): List[Token] = {
    for (
      t1 <- last
        t2 <- prev
    ) t1.appendMatchesFrom(t2)
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
    println("")
    println(">>>  " + s.fact.fact)
    val result = go(this)
    println("    " + result._1)
    println("<<< " + result._2)

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
    this.~>(exists(pred))
  }
}

case class Matched(tokens: List[Token], next: EventPattern) extends EventPattern {
  override def toString() =
    "[" + (tokens.map(x => x.fact.fact).mkString(",")) + "]"
}
case class Waiting(f: WaitFunc) extends EventPattern {
  override def toString() =
    "" + f + ""
}
case class Halted() extends EventPattern
trait WaitFunc {
  def apply(t: Token): EventPattern
}
case class AwaitSecond(first: Matched, second: Waiting) extends WaitFunc {
  def apply(t: Token) = {
    val next =
      first.tokens
        .map(prevMatch => second.f(t.appendMatchesFrom(prevMatch)))
        .foldLeft1Opt(_ ++ _).getOrElse(Halted())

    next match {
      case m: Matched => (first ~> next) ++ (first ~> second)
      case _          => (first ~> second)
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
      case _          => (first ~> second)
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

  override def toString() = "?"
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


case class Until(pattern: EventPattern, until: Waiting) extends WaitFunc {
  def apply(t: Token) = {
    val untilNext =
      until.f(t)

    untilNext match {
      case Halted()              => Halted()
      case Matched(untilTokens, next) => pattern match {
        case Halted()                      => Halted()
        case w@Waiting(g)                  => Halted()
        case Matched(matched, patternNext) => Matched(Token.append(untilTokens,matched), Waiting(this))
      }
      case w@Waiting(f)          => pattern match {
        case Halted()              => Halted()
        case Waiting(f)            => Waiting(Until(f(t), until))
        case Matched(tokens, next) => Waiting(Until(Waiting(Append(tokens, next)), until))

      }
    }
  }
}

case class Append(history: List[Token], next: EventPattern) extends WaitFunc {
  def apply(t: Token) = {
    next match {
      case Halted()              => Halted()
      case Matched(tokens, next) => Matched(Token.append(tokens, history), next)
      case w@Waiting(f)          => Waiting(Append(history, w))
    }
  }
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


trait Matcher[A] {
  def apply(t: Token): (Matcher[A], List[A])
}

object Matcher {
  implicit def matcherMonoid[A]: Monoid[Matcher[A]] =
    Monoid.instance({ case (m1: Matcher[A], m2: Matcher[A]) => AndMatcher(m1, m2)}, ZeroMatcher[A]())

  def matcher[A](pattern: EventPattern)(f: Token => Option[A]) =
    SingleMatcher(pattern, f)

}

case class ZeroMatcher[A]() extends Matcher[A] {
  def apply(t: Token) = (this, Nil)
}

case class SingleMatcher[A](pattern: EventPattern, f: Token => Option[A]) extends Matcher[A] {
  def apply(t: Token): (Matcher[A], List[A]) = {
    val (next, tokens) = pattern.parse(t)

    (SingleMatcher[A](next, f), tokens.map(f).collect { case Some(x) => x})
  }
}

case class AndMatcher[A](one: Matcher[A], other: Matcher[A]) extends Matcher[A] {
  def apply(t: Token) = {
    val (next1, tokens1) = one(t)
    val (next2, tokens2) = other(t)

    (AndMatcher(next1, next2), tokens1 ::: tokens2)
  }
}




