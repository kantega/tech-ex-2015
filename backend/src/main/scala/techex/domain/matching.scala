package techex.domain


import org.joda.time.{Hours, Instant, Duration}

import scalaz._, Scalaz._
import matching._

object matching {
  type Pred = Token => Boolean
  implicit val dfaultExpireDutararion =
    Hours.THREE.toStandardDuration

  def exists(pred: Pred): EventPattern = {
    Single(pred)
  }

}

object predicates {

  case class PredOps(pred: Pred) {
    def and(other: Pred) =
      predicates.and(pred, other)


    def or(other: Pred) =
      predicates.or(pred, other)

    def ~>(other: Pred) =
      exists(pred) ~> exists(other)

    def fBy(other: Pred) =
      ~>(other)
  }

  implicit def toInfixOps(pred: Pred): PredOps =
    PredOps(pred)

  def not(pred: Pred): Pred =
    ctx => !pred(ctx)

  def !!(pred: Pred): Pred =
    not(pred)

  def and(one: Pred, other: Pred): Pred =
    ctx => one(ctx) && other(ctx)

  def or(one: Pred, other: Pred): Pred = {
    ctx => one(ctx) || other(ctx)
  }

  def visited(area: Area) =
    fact({ case Entered(a) if a === area => true})


  def ctx(f: PartialFunction[(FactUpdate, List[FactUpdate]), Boolean]) = {
    val pred: Pred =
      ctx =>
        if (f.isDefinedAt((ctx.fact, ctx.matches)))
          f((ctx.fact, ctx.matches))
        else
          false

    pred
  }


  def update(f: PartialFunction[FactUpdate, Boolean]) = {
    val pred: Pred =
      ctx =>
        if (f.isDefinedAt(ctx.fact))
          f(ctx.fact)
        else
          false


    pred
  }

  def fact(f: PartialFunction[Fact, Boolean]) = {
    val pred: Pred =
      ctx =>
        if (f.isDefinedAt(ctx.fact.fact))
          f(ctx.fact.fact)
        else
          false
    pred
  }

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

  override def toString: String =
    "Token(fact:" + fact.fact + " - matches: [" + matches.reverse.map(_.fact).mkString(" ~> ") + "])"
}

trait EventPattern {

  def parse(s: Token): (EventPattern, List[Token])

  def ++(p: => EventPattern): EventPattern =
    (this, p) match {
      case (Exhausted(), Exhausted()) => Exhausted()
      case (Exhausted(), other)       => other
      case (one, Exhausted())         => one
      case (one, other)               => And(one, other)
    }

  def ~>(p: => EventPattern)(implicit validDuraion: Duration): EventPattern = {
    (this, p) match {
      case (Exhausted(), _) => Exhausted()
      case (_, Exhausted()) => Exhausted()
      case _                => FBy(this, p, validDuraion)
    }
  }
}

case class FBy(one: EventPattern, other: EventPattern, duration: Duration) extends EventPattern {
  def parse(t: Token) = {

    val (oneNext, oneOutput) =
      one.parse(t)

    val waiting: List[EventPattern] =
      oneOutput
        .map(token => AwaitOther(other, token.matches))

    val waitingCollapsed =
      waiting.foldLeft1Opt {_ ++ _}.getOrElse(Exhausted())

    val expire =
      t.fact.info.instant.plus(duration).toInstant
    (Expires(waitingCollapsed, expire) ++ oneNext.~>(other)(duration), Nil)
  }
}

case class AwaitOther(pattern: EventPattern, matches: List[FactUpdate]) extends EventPattern {
  def parse(s: Token) = {
    val (next, tokens) = pattern.parse(Token(s.fact, matches ::: s.matches))
    next match {
      case Exhausted() => (Exhausted(), tokens)
      case _           => (AwaitOther(next, matches), tokens)
    }
  }
}

case class Expires(pattern: EventPattern, expires: Instant) extends EventPattern {
  def parse(s: Token) = {
    if (s.fact.info.instant.isAfter(expires))
      (Exhausted(), Nil)
    else {
      val (next, tokens) = pattern.parse(s)
      (Expires(next, expires), tokens)

    }
  }
}
case class Single(pred: Pred) extends EventPattern {
  def parse(s: Token) =
    if (pred(s))
      (this, List(s.addToken))
    else
      (this, Nil)
}

case class Exhausted() extends EventPattern {
  def parse(s: Token): (EventPattern, List[Token]) = {
    (this, Nil)
  }
}


case class And(one: EventPattern, other: EventPattern) extends EventPattern {
  def parse(t: Token) = {
    val (oneNext, oneTokens) = one.parse(t)
    val (twoNext, twoTokens) = other.parse(t)

    (oneNext ++ twoNext, oneTokens ::: twoTokens)
  }
}


trait Matcher[A] {
  def apply(t: Token): (Matcher[A], List[A])
}

object Matcher {
  implicit def matcherMonoid[A]: Monoid[Matcher[A]] =
    Monoid.instance({case (m1: Matcher[A], m2: Matcher[A]) => AndMatcher(m1, m2)}, ZeroMatcher[A]())

  def matcher[A](pattern: EventPattern)(f: Token => Option[A]) =
    SingleMatcher(pattern, f)

}

case class ZeroMatcher[A]() extends Matcher[A] {
  def apply(t: Token) = (this, Nil)
}

case class SingleMatcher[A](pattern: EventPattern, f: Token => Option[A]) extends Matcher[A] {
  def apply(t: Token): (Matcher[A], List[A]) = {
    val (next, tokens) = pattern.parse(t)

    (SingleMatcher[A](next, f), tokens.map(f).collect{case Some(x)=>x})
  }
}

case class AndMatcher[A](one: Matcher[A], other: Matcher[A]) extends Matcher[A] {
  def apply(t: Token) = {
    val (next1, tokens1) = one(t)
    val (next2, tokens2) = other(t)

    (AndMatcher(next1, next2), tokens1 ::: tokens2)
  }
}




